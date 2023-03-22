package com.sbz.web3authdemoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.web3auth.core.Web3Auth
import com.web3auth.core.types.*
import java8.util.concurrent.CompletableFuture
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService


class MainActivity : AppCompatActivity() {

    private lateinit var web3Auth: Web3Auth
    private lateinit var sessionId: String // <-- Stores the Web3Auth's sessionId.
    private lateinit var web3: Web3j
    private lateinit var credentials: Credentials
    private val rpcUrl = "https://rpc.ankr.com/eth"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val clientId = getString(R.string.web3auth_project_id)
        web3Auth = Web3Auth(
            Web3AuthOptions(
                context = this,
                clientId = clientId,
                network = Web3Auth.Network.TESTNET,
                redirectUrl = Uri.parse("com.sbz.web3authdemoapp://auth"),
                whiteLabel = WhiteLabelData(
                     "Nexera Android Example", null, null, "en", true,
                     hashMapOf(
                         "primary" to "#229954"
                     )
                ),
                // Optional loginConfig object
                loginConfig = hashMapOf("jwt" to LoginConfigItem(
                    verifier = "dua-custom-jwt", // get it from web3auth dashboard
                    typeOfLogin = TypeOfLogin.JWT,
                    clientId = clientId,
                )),
            )
        )

        // Handle user signing in when app is not alive
        web3Auth.setResultUrl(intent?.data)

        // Call sessionResponse() in onCreate() to check for any existing session.
        val sessionResponse: CompletableFuture<Web3AuthResponse> = web3Auth.sessionResponse()
        sessionResponse.whenComplete { loginResponse, error ->
            if (error == null) {
                sessionId = loginResponse.sessionId.toString()
                credentials = Credentials.create(sessionId)
                web3 = Web3j.build(HttpService(rpcUrl))
                println(loginResponse)
                reRender(loginResponse)
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
                // Ideally, you should initiate the login function here.
            }
        }

        // Setup UI and event handlers
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener { signIn() }

        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener { signOut() }

        reRender(Web3AuthResponse())
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Handle user signing in when app is active
        web3Auth.setResultUrl(intent?.data)
    }

    private fun signIn() {
        val selectedLoginProvider = Provider.JWT   // Can be GOOGLE, FACEBOOK, TWITCH etc.
        // tokenId should be fetched from => https://compliantapp.nexera.id/auth/tokens?email=desarrollo@wooy.co
        val tokenId = "eyJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiIsImtpZCI6IkRjMjVwbTFsNnhGY01XcEpjSHNHY1N6WDZFd2JPZk1FdDFUa0tNeEtLQU0ifQ.eyJzdWIiOiJDdXN0b20gSldUIGZvciBXZWIzQXV0aCBDdXN0b20gQXV0aCIsImVtYWlsIjoiZGVzYXJyb2xsb0B3b295LmNvIiwiYXVkIjoidXJuOm15LXJlc291cmNlLXNlcnZlciIsImlzcyI6Imh0dHBzOi8vbXktYXV0aHotc2VydmVyIiwiaWF0IjoxNjc5NDQ5MDg3LCJleHAiOjE2Nzk0NTI2ODd9.felWbhf7LNpT6gYk9JjDhdcVoohpksBlczQG_jhEOcCj7hzV8o54UmltkCsdKtaUHK45-ZUQx6epxSmcbrcsXNdPE8BWx9RRLBgezWQ9yQqdzqb8wURdYrDx2ns-G2zbRihmNkruQ3x1hxrwgdwLmX5MBMRkmvDz9XR0xeLBPeneN6B1ZXf3zCgQnnlcFp96Qk0ql0TB1nY37JzDqY2rPVAq57tnQ5BfY6Qv9t2sma-MH8O8fsW7Rj5epeYe0paH_lpGqerBE8Suz6rsqdLLk73xvbTh4luF1BiMQzp8NvmgwZwEYYTS-mJZfsG44RszymxSHRzWL_bfqkDWEbeWuQ"
        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.login(LoginParams(selectedLoginProvider,
                extraLoginOptions = ExtraLoginOptions(
                    id_token = tokenId,
                    domain = "http://localhost:3000", // domain of your  app
                    verifierIdField = "email", // The field in jwt token which maps to verifier id.
            )
        ))

        loginCompletableFuture.whenComplete { loginResponse, error ->
            if (error == null) {
                // Set the sessionId from Web3Auth in App State
                // This will be used when making blockchain calls with Web3j
                sessionId = loginResponse.sessionId.toString()
                // Sets the credentials and Web3j instance.
                credentials = Credentials.create(sessionId)
                web3 = Web3j.build(HttpService(rpcUrl))
                println(loginResponse)
                reRender(loginResponse)
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong" )
            }
        }
    }

    private fun signOut() {
        val logoutCompletableFuture =  web3Auth.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                reRender(Web3AuthResponse())
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong" )
            }
        }
        recreate()
    }

    private fun reRender(web3AuthResponse: Web3AuthResponse) {
        val contentTextView = findViewById<TextView>(R.id.contentTextView)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        val key = web3AuthResponse.privKey
        val userInfo = web3AuthResponse.userInfo
        println(userInfo)
        if (key is String && key.isNotEmpty()) {
            contentTextView.text = web3AuthResponse.toString()
            contentTextView.visibility = View.VISIBLE
            signInButton.visibility = View.GONE
            signOutButton.visibility = View.VISIBLE
        } else {
            contentTextView.text = getString(R.string.not_logged_in)
            contentTextView.visibility = View.GONE
            signInButton.visibility = View.VISIBLE
            signOutButton.visibility = View.GONE
        }
    }
}