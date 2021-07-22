package com.zapps.coroutinecrashdemos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.zapps.coroutinecrashdemos.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        val service: GithubService= retrofit.create(GithubService::class.java)

        binding.successButton.setOnClickListener {
            callApiNormally(service)
        }


        binding.rxAwaitWithoutIO.setOnClickListener {
            launchApiOnMain(service)
        }

        // 앱이 강제 종료됩니다.
        binding.asyncCrashButton.setOnClickListener {
            crashThisApp()
        }

        binding.asyncCatchButton.setOnClickListener {
            catchErrorByWrappingNewContext()
        }
    }

    private fun callApiNormally(service: GithubService) {
        lifecycleScope.launch {
            try {
                val repos = service.reposSuspend("wlals822")
                binding.resultText.text = repos.joinToString(", ")
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "catched error", Toast.LENGTH_SHORT).show()
                    binding.resultText.text = ex.toString()
                }
            }
        }
    }

    /**
     * App crash by async block's error propagation
     * async, launch
     */
    private fun crashThisApp() {
        lifecycleScope.launch {
            try {
                async {
                    throw error("shot error")
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "catched error", Toast.LENGTH_SHORT).show()
                    binding.resultText.text = ex.toString()
                }
            }
        }
    }

    /**
     * Though async block failed, launch block is not canceled.
     * withContext -> Failed but catched by try-catch
     */
    private fun catchErrorByWrappingNewContext() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) { // catch by new root = runblocking, coroutineScope, GlobalScope.launch
                    async {
                        throw error("shot error")
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "catched error", Toast.LENGTH_SHORT).show()
                    binding.resultText.text = ex.toString()
                }
            }
        }
    }

    /**
     * If you use rx await on main dispatcher, it throws NetworkOnMainThreadException
     */
    private fun launchApiOnMain(service: GithubService) {
        lifecycleScope.launch {
            try {
                service.reposSingle("wlals822").await()
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "catched error", Toast.LENGTH_SHORT).show()
                    binding.resultText.text = ex.toString()
                }
            }
        }
    }
}