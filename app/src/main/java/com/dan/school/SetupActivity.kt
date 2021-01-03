package com.dan.school

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.dan.school.authentication.AuthenticationActivity
import com.dan.school.authentication.AuthenticationFragment
import com.dan.school.setup.ProfileSetupFragment
import com.dan.school.setup.SetupViewPagerFragment
import com.dan.school.authentication.SignInFragment
import com.dan.school.setup.SetupFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SetupActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        auth = Firebase.auth
        database = Firebase.database

        sharedPref = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(
                    R.id.frameLayoutSetup,
                    SetupFragment()
                ).commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTHENTICATION_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            when (data?.getIntExtra(AuthenticationActivity.RESULT, -1)) {
                AuthenticationActivity.AUTHENTICATION_CANCELLED -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.frameLayoutSetup,
                            ProfileSetupFragment.newInstance()
                        ).commit()
                }
                AuthenticationActivity.AUTHENTICATION_SUCCESS -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(
                            R.id.frameLayoutSetup,
                            SetupViewPagerFragment()
                        ).commit()
                }
                AuthenticationActivity.AUTHENTICATION_WITH_GOOGLE_SUCCESS -> {
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.frameLayoutSetup,
                            ProfileSetupFragment.newInstance(
                                data.getStringExtra(School.NICKNAME) ?: "",
                                data.getStringExtra(School.FULL_NAME) ?: ""
                            )
                        ).commit()
                }
            }
        }
    }

    fun buttonGetStartedClicked() {
        startActivityForResult(
            Intent(this, AuthenticationActivity::class.java),
            AUTHENTICATION_ACTIVITY_REQUEST_CODE
        )
    }

    fun profileSetupDone(nickname: String, fullName: String) {
        with(sharedPref.edit()) {
            putString(School.NICKNAME, nickname)
            putString(School.FULL_NAME, fullName)
            apply()
        }
        if (auth.currentUser != null) {
            val map: MutableMap<String, Any> = HashMap()
            map[School.NICKNAME] = nickname
            map[School.FULL_NAME] = fullName
            database.reference.child(School.USERS).child(auth.currentUser!!.uid)
                .updateChildren(map).addOnCompleteListener {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(
                            R.id.frameLayoutSetup,
                            SetupViewPagerFragment()
                        ).commit()
                }
        }
    }

    fun setupDone() {
        with(sharedPref.edit()) {
            putBoolean(School.IS_SETUP_DONE, true)
            apply()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val AUTHENTICATION_ACTIVITY_REQUEST_CODE = 1
    }
}