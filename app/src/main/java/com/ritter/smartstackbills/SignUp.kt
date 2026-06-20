package com.ritter.smartstackbills

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var signUpButton: Button
    private lateinit var googleButton: Button
    private lateinit var loginText: TextView
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
        setContentView(R.layout.activity_sign_up)

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
        signUpButton = findViewById(R.id.btnSign)
        googleButton = findViewById(R.id.btnGoogle)
        loginText = findViewById(R.id.txtLogin)
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
        loginText.setOnClickListener {
            startActivity(Intent(this, LogIn::class.java))
        }

        signUpButton.setOnClickListener {
            performSignUp()
        }

        googleButton.setOnClickListener {
            signInWithGoogle()
        }

        passwordField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performSignUp()
                true
            } else {
                false
            }
        }
    }

    private fun performSignUp() {
        val inputEmail = emailField.text.toString().trim()
        val inputPassword = passwordField.text.toString()

        if (!validateEmail(inputEmail) || !validatePassword(inputPassword)) {
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(inputEmail, inputPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    completeSuccessfulAuth(inputEmail, "password")
                } else {
                    setLoading(false)
                    Toast.makeText(this, authErrorMessage(task.exception), Toast.LENGTH_SHORT).show()
                }
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
        return when {
            password.isEmpty() -> {
                passwordField.error = getString(R.string.password_required)
                false
            }
            password.length < 6 -> {
                passwordField.error = getString(R.string.password_too_short)
                false
            }
            else -> true
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
        signUpButton.isEnabled = !isLoading
        googleButton.isEnabled = !isLoading
        loginText.isEnabled = !isLoading
        emailField.isEnabled = !isLoading
        passwordField.isEnabled = !isLoading
    }

    private fun authErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseNetworkException -> getString(R.string.auth_network_error)
            is FirebaseAuthUserCollisionException -> getString(R.string.account_exists_error)
            is FirebaseAuthWeakPasswordException -> getString(R.string.password_too_short)
            else -> getString(R.string.auth_generic_error)
        }
    }

    private fun completeSuccessfulAuth(email: String?, provider: String) {
        FirebaseUserProfile.ensureExists(auth.currentUser, provider)
            ?.addOnCompleteListener {
                setLoading(false)
                navigateToMainMenu(email)
            }
            ?: run {
                setLoading(false)
                navigateToMainMenu(email)
            }
    }

    private fun navigateToMainMenu(email: String?) {
        startActivity(AuthUtils.mainMenuIntent(this, email))
        finish()
    }
}
