package com.zapps.coroutinecrashdemos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.zapps.coroutinecrashdemos.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.rx2.await
import java.lang.Exception
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Logger


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

        val service: GithubService = retrofit.create(GithubService::class.java)

        binding.successButton.setOnClickListener {
            callApiNormally(service)
        }


        binding.rxAwaitWithoutIO.setOnClickListener {
            launchApiOnMain(service)
        }

        // 앱이 강제 종료됩니다.
        binding.asyncCrashButton.setOnClickListener {
            crashThisApp(service)
        }

        binding.asyncCatchButton.setOnClickListener {
            catchErrorByWrappingNewContext()
        }
        binding.testSuperviorButton.setOnClickListener {
            featureOfSupervisorJob(service)
        }
        binding.useScopeIndivisualButton.setOnClickListener {
            testAsyncByParentScope(service)
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

    private fun testAsyncByParentScope(service: GithubService) {
        // launch 는 바로 터짐, async는 root를 사용하기 때문에 지연됨.
        val deferredError = lifecycleScope.async {
            error("shot eror")
        }

        val deferredRepos = lifecycleScope.async {
            delay(1000)
            val result = service.reposSuspend("wlals822")
            withContext(Dispatchers.Main) {
                binding.resultText.text = result.joinToString()
            }
        }

        // 이런 상태에서 job1을 예외처리 해도 job2 의 결과는 출력되지 않는다.
//        val scope = CoroutineScope(Job())
//        val notSupervisorJob1 = scope.async {
//            error("shot error")
//        }
//
//        val notSupervisorJob2 = scope.launch {
//            val result = service.reposSuspend("wlals822")
//            withContext(Dispatchers.Main) {
//                binding.resultText.text = result.joinToString()
//            }
//        }
        // defferedError에서 바로 예외가 터져서 deferredError는 취소되지만 깃헙 요청은 성공적으로 진행함.
        lifecycleScope.launch {
            try {
//                notSupervisorJob1.await()
//                deferredError.await()
//                deferredRepos.await()
            } catch (ex: Exception) {
                Toast.makeText(this@MainActivity, "catched error", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /**
     * App crash by async block's error propagation
     * async, launch
     */
    private fun crashThisApp(service: GithubService) {

        //  cannot catch exception
        try {
            lifecycleScope.launch {
                try {
                    async(Dispatchers.IO) { // uses new job (not supervisorJob)
                        error("shot error")
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "catched error", Toast.LENGTH_SHORT).show()
                        binding.resultText.text = ex.toString()
                    }
                }
            }
        } catch (ex: Exception) {
            Toast.makeText(this@MainActivity, "catched error", Toast.LENGTH_SHORT).show()
            binding.resultText.text = ex.toString()
        }
    }

    /**
     * Though async block failed, launch block is not canceled.
     * withContext -> Failed but catched by try-catch
     */
    private fun catchErrorByWrappingNewContext() {
        lifecycleScope.launch {
            Log.d("job info", coroutineContext.job.toString())
            try {
                // When extract method by ide, it has different method signature
//                runSuspendedAction()
//                runLaunchJob()
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "catched error: $ex", Toast.LENGTH_SHORT).show()
                    binding.resultText.text = ex.toString()
                }
            }
        }
    }

    // Uses CoroutineScope as a Receiver
    private fun CoroutineScope.runLaunchJob() {
        launch {
            error("error on launch")
        }
    }

    // No Parameter
    private suspend fun runSuspendedAction() {
        withContext(Dispatchers.Main) {
            Log.d("job info", coroutineContext.job.toString())
            async {
                error("shot error")
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

    // app crash
    private fun featureOfSupervisorJob(service: GithubService) {
        lifecycleScope.launch {
            launch {
                try {
                    async {
                        service.reposSuspend("wlals822")
                    }
                } catch (ex: Exception) {
                    Toast.makeText(this@MainActivity, "api error", Toast.LENGTH_SHORT).show()
                }
            }
            launch {
                delay(1000)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "message after delay", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}