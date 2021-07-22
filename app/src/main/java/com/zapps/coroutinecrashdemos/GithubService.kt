package com.zapps.coroutinecrashdemos

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubService {
    @GET("users/{user}/repos")
    fun reposSingle(@Path("user") user: String): Single<List<Repo>>

    @GET("users/{user}/repos")
    suspend fun reposSuspend(@Path("user") user: String): List<Repo>
}


data class Repo(
    val name: String,
    val full_name: String,
)