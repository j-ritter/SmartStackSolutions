package com.ritter.smartstackbills

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider

class LogIn : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var googleButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var signUpText: TextView
    private lateinit var progressAuth: ProgressBar
    private var isPasswordVisible = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            firebaseAuthWithGoogle(task.getResult(ApiException::class.java))
        } catch (e: ApiException) {
            setLoading(false)
            if (e.statusCode != 12501) {
                Toast.makeText(this, R.string.auth_generic_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(AuthUtils.mainMenuIntent(this))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in)

        sharedPreferences = getSharedPreferences("SmartStackBillsPrefs", MODE_PRIVATE)
        googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        bindViews()
        setupInsets()
        setupPasswordToggle()
        setupActions()
    }

    private fun bindViews() {
        emailField = findViewById(R.id.edtEmail)
        passwordField = findViewById(R.id.edtPassword)
        loginButton = findViewById(R.id.btnlog)
        googleButton = findViewById(R.id.btnGoogleLogin)
        forgotPasswordText = findViewById(R.id.txtForgotPassword)
        signUpText = findViewById(R.id.txtSign)
        progressAuth = findViewById(R.id.progressAuth)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupActions() {
        signUpText.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        forgotPasswordText.setOnClickListener {
            sendPasswordReset()
        }

        loginButton.setOnClickListener {
            performLogin()
        }

        googleButton.setOnClickListener {
            signInWithGoogle()
        }

        passwordField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else {
                false
            }
        }
    }

    private fun performLogin() {
        val emailInput = emailField.text.toString().trim()
        val passwordInput = passwordField.text.toString()

        if (!validateEmail(emailInput) || !validatePassword(passwordInput)) {
            return
        }

        setLoading(true)
        auth.signInWithEmailAndPassword(emailInput, passwordInput)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    completeSuccessfulAuth(emailInput, "password")
                } else {
                    setLoading(false)
                    Toast.makeText(this, authErrorMessage(task.exception), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendPasswordReset() {
        val emailInput = emailField.text.toString().trim()
        if (!validateEmail(emailInput)) {
            return
        }

        setLoading(true)
        auth.sendPasswordResetEmail(emailInput)
            .addOnCompleteListener { task ->
                setLoading(false)
                val message = if (task.isSuccessful) {
                    getString(R.string.password_reset_sent)
                } else {
                    getString(R.string.password_reset_failed)
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun signInWithGoogle() {
        setLoading(true)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val idToken = account?.idToken
        if (idToken == null) {
            setLoading(false)
            Toast.makeText(this, R.string.auth_generic_error, Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    completeSuccessfulAuth(account.email ?: AuthUtils.currentUserEmail(), "google")
                } else {
                    setLoading(false)
                    Toast.makeText(this, authErrorMessage(task.exception), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateEmail(email: String): Boolean {
        emailField.error = null
        return when {
            email.isEmpty() -> {
                emailField.error = getString(R.string.email_required)
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailField.error = getString(R.string.email_invalid)
                false
            }
            else -> true
        }
    }

    private fun validatePassword(password: String): Boolean {
        passwordField.error = null
        return if (password.isEmpty()) {
            passwordField.error = getString(R.string.password_required)
            false
        } else {
            true
        }
    }

    private fun setupPasswordToggle() {
        passwordField.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && event.rawX >= passwordField.right - passwordField.compoundPaddingEnd) {
                togglePasswordVisibility()
                true
            } else {
                false
            }
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        passwordField.transformationMethod = if (isPasswordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordField.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            if (isPasswordVisible) R.drawable.ic_visibility_24 else R.drawable.ic_visibility_off_24,
            0
        )
        passwordField.setSelection(passwordField.text.length)
    }

    private fun setLoading(isLoading: Boolean) {
        progressAuth.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
        googleButton.isEnabled = !isLoading
        forgotPasswordText.isEnabled = !isLoading
        signUpText.isEnabled = !isLoading
        emailField.isEnabled = !isLoading
        passwordField.isEnabled = !isLoading
    }

    private fun authErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseNetworkException -> getString(R.string.auth_network_error)
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException -> getString(R.string.auth_generic_error)
            else -> getString(R.string.auth_generic_error)
        }
    }

    private fun completeSuccessfulAuth(email: String?, provider: String) {
        FirebaseUserProfile.ensureExists(auth.currentUser, provider)
            ?.addOnCompleteListener {
                setLoading(false)
                handleSuccessfulLogin(email)
            }
            ?: run {
                setLoading(false)
                handleSuccessfulLogin(email)
            }
    }

    private fun handleSuccessfulLogin(email: String?) {
        if (isFirstLoginForCurrentUser()) {
            showWelcomeDialog(email)
        } else {
            navigateToMainMenu(email)
        }
    }

    private fun showWelcomeDialog(email: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_welcome, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            dialog.dismiss()
            setFirstLoginFlagForCurrentUser(false)
            navigateToMainMenu(email)
        }

        dialog.show()
    }

    private fun navigateToMainMenu(email: String?) {
        startActivity(AuthUtils.mainMenuIntent(this, email))
        finish()
    }

    private fun firstLoginKey(): String {
        val uid = auth.currentUser?.uid ?: "unknown"
        return "isFirstLogin_$uid"
    }

    private fun isFirstLoginForCurrentUser(): Boolean {
        return sharedPreferences.getBoolean(firstLoginKey(), true)
    }

    private fun setFirstLoginFlagForCurrentUser(isFirst: Boolean) {
        sharedPreferences.edit().putBoolean(firstLoginKey(), isFirst).apply()
    }
}
