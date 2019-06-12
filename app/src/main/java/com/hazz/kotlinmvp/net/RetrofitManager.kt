package com.hazz.kotlinmvp.net

import com.hazz.kotlinmvp.Constants
import com.hazz.kotlinmvp.MyApplication
import com.hazz.kotlinmvp.api.ApiService
import com.hazz.kotlinmvp.api.UrlConstant
import com.hazz.kotlinmvp.utils.AppUtils
import com.hazz.kotlinmvp.utils.NetworkUtil
import com.hazz.kotlinmvp.utils.Preference
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Created by xuhao on 2017/11/16.
 *
 */

object RetrofitManager {

    val service: ApiService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getRetrofit().create(ApiService::class.java)
    }

    private var token: String by Preference("token", "")

    /**
     * 设置公共参数
     */
    private fun addQueryParameterInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val request: Request
            val modifiedUrl = originalRequest.url().newBuilder()
                    // Provide your custom parameter here
                    .addQueryParameter("udid", "d2807c895f0348a180148c9dfa6f2feeac0781b5")
                    .addQueryParameter("deviceModel", AppUtils.getMobileModel())
                    .build()
            request = originalRequest.newBuilder().url(modifiedUrl).build()
            chain.proceed(request)
        }
    }

    /**
     * 设置头
     */
    private fun addHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                    // Provide your custom header here
                    .header("token", token)
                    .method(originalRequest.method(), originalRequest.body())
            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }

    /**
     * 设置缓存
     */
    private fun addCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            if (!NetworkUtil.isNetworkAvailable(MyApplication.context)) {
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build()
            }
            val response = chain.proceed(request)
            if (NetworkUtil.isNetworkAvailable(MyApplication.context)) {
                val maxAge = 0
                // 有网络时 设置缓存超时时间0个小时 ,意思就是不读取缓存数据,只对get有用,post没有缓冲
                response.newBuilder()
                        .header("Cache-Control", "public, max-age=$maxAge")
                        // 清除头信息，因为服务器如果不支持，会返回一些干扰信息，不清除下面无法生效
                        .removeHeader("Retrofit")
                        .build()
            } else {
                // 无网络时，设置超时为4周  只对get有用,post没有缓冲
                val maxStale = 60 * 60 * 24 * 28
                response.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                        .removeHeader("nyn")
                        .build()
            }
            response
        }
    }

    /**
     * 设置日志
     */
    private fun addLogInterceptor(): Interceptor {
        return HttpLoggingInterceptor().also {
            // 可以设置请求过滤的水平,body,basic,headers
            it.level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun getRetrofit(): Retrofit {
        // 获取retrofit的实例
        /*
            Retrofit:可以理解为OkHttp的加强版，它也是一个网络加载框架，底层是使用OKHttp封装的。
                网络请求的工作本质上是OkHttp完成，而 Retrofit 仅负责网络请求接口的封装。
            1、超级解耦
            2、可以配置不同HttpClient来实现网络请求，如OkHttp、HttpClient...
            3、支持同步、异步和RxJava
            4、可以配置不同的反序列化工具来解析数据，如json、xml...
            5、请求速度快，使用非常方便灵活
         */
        return Retrofit.Builder()
                // 设置网络请求的Url地址
                .baseUrl(UrlConstant.BASE_URL)
                .client(getOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                // 设置数据解析器
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .addInterceptor(addQueryParameterInterceptor()) // 参数添加
                .addInterceptor(addHeaderInterceptor()) // token过滤
                .addInterceptor(addCacheInterceptor()) // 缓存设置
                .addInterceptor(addLogInterceptor()) // 日志,所有的请求响应
                .cache(Cache(File(MyApplication.context.cacheDir,
                        Constants.CACHE_DIR), 1024 * 1024 * 50))  // 添加缓存目录与大小
                .connectTimeout(60L, TimeUnit.SECONDS)
                .readTimeout(60L, TimeUnit.SECONDS)
                .writeTimeout(60L, TimeUnit.SECONDS)
                .build()
    }

}
