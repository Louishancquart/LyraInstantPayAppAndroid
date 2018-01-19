package com.lyranetwork.demo.epos.Activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.lyranetwork.demo.epos.BuildConfig
import com.lyranetwork.demo.epos.R
import com.lyranetwork.demo.epos.Util.MyContextWrapper
import kotlinx.android.synthetic.main.credentials.*
import java.util.*

/**
 * Display the success payment
 */
class CredentialsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.credentials)

        // Listeners
        initListeners()


        textViewPoweredByCredentials.setText(resources.getString(R.string.powered_by_lyra) + " v" + BuildConfig.VERSION_NAME)
    }

    /**
     * Listeners
     */
    @Suppress("DEPRECATION")
    private fun initListeners() {

        //Init EditText ShopID
        editTextShopID?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                updateShopIDEditTextState()
                storeShopID(editTextShopID.text.toString(), applicationContext)
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })


        //Init EditText orderID
        editTextCertificate?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                updateCertificateEditTextState()
                storeCertificate(editTextCertificate.text.toString(), applicationContext)
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        //Login
        buttonLogin?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Go to MainActivity
                storeCertificate(editTextCertificate.text.toString(), applicationContext)
                storeShopID(editTextShopID.text.toString(), applicationContext)
                goToMainActivity()
                finish()
            }
        })

        //Init Button param
        imageButtonLangCredentials?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val items = arrayOf<CharSequence>(
                        Html.fromHtml("<font color='#3775ba'>" + getString(R.string.english) + "</font>"),
                        Html.fromHtml("<font color='#3775ba'>" + getString(R.string.french) + "</font>"),
                        Html.fromHtml("<font color='#3775ba'>" + getString(R.string.spanish) + "</font>"))
                val builder = AlertDialog.Builder(this@CredentialsActivity)
                var title = TextView(this@CredentialsActivity)
                title.setText(Html.fromHtml("<font color='#3775ba'>" + getString(R.string.language) + "</font>"))
                title.setPadding(10, 35, 10, 10)
                title.setGravity(Gravity.CENTER)
                title.setTextSize(24.0F)
                builder.setCustomTitle(title)
                builder.setItems(items) { _, item ->
                    when (item) {
                        0 -> if (!Locale.getDefault().language.equals(
                                MainActivity.LanguagesEnum.identifier(MainActivity.LanguagesEnum.ENGLISH))) {
                            MainActivity.storeLang(applicationContext, MainActivity.LanguagesEnum.ENGLISH);reloadCredentialsActivity()
                        }
                        1 -> if (!Locale.getDefault().language.equals(MainActivity.LanguagesEnum.identifier(MainActivity.LanguagesEnum.FRENCH))) {
                            MainActivity.storeLang(applicationContext, MainActivity.LanguagesEnum.FRENCH);reloadCredentialsActivity()
                        }
                        2 -> if (!Locale.getDefault().language.equals(
                                MainActivity.LanguagesEnum.identifier(MainActivity.LanguagesEnum.SPANISH))) {
                            MainActivity.storeLang(applicationContext, MainActivity.LanguagesEnum.SPANISH);reloadCredentialsActivity()
                        }
                    }
                }
                builder.create().show()
            }
        })
    }

    /**
     * Display error on ShopID editText if value not valid
     */
    private fun updateShopIDEditTextState() {
        if (TextUtils.isEmpty(editTextShopID.text)) {
            editTextShopID.error = resources.getString(R.string.invalid_shop_id)
        }
    }

    /**
     * Display error on Certificae editText if value not valid
     */
    private fun updateCertificateEditTextState() {
        if (TextUtils.isEmpty(editTextShopID.text)) {
            editTextCertificate.error = resources.getString(R.string.invalid_certificate)
        }
    }


    /**
     * Go to main activity
     */
    private fun goToMainActivity() {
        val refresh = Intent(this@CredentialsActivity, MainActivity::class.java)
        refresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptionsCompat.makeCustomAnimation(applicationContext, android.R.anim.fade_in, android.R.anim.fade_out)
        ActivityCompat.startActivity(this@CredentialsActivity, refresh, options.toBundle())
        finish()
    }

    /**
     * Force selected language
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase, MainActivity.LanguagesEnum.Companion.identifier(MainActivity.Companion.getLang(newBase))))
        MainActivity.Companion.setLanguageForApp(MainActivity.LanguagesEnum.identifier(MainActivity.getLang(newBase)), newBase)
    }


    /**
     * Reload MainActivity to update language
     */
    private fun reloadCredentialsActivity() {
        val refresh = Intent(this@CredentialsActivity, CredentialsActivity::class.java)
        refresh.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptionsCompat.makeCustomAnimation(applicationContext, android.R.anim.fade_in,
                android.R.anim.fade_out)
        ActivityCompat.startActivity(this@CredentialsActivity, refresh, options.toBundle())
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        // Erase values on destroy
        storeShopID(editTextShopID.text.toString(), applicationContext)
        storeCertificate(editTextCertificate.text.toString(), applicationContext)
        if (isFinishing()) {
            //call some method
        }
    }

    override fun onPause() {
        super.onPause()
        // Store values onPause (values retrieved after language switched)
    }

    companion object {
        private var MY_PREFS_NAME = "PayApp"
        /**
         * Store shopID on SharedPreferences
         */
        fun storeShopID(shopID: String, context: Context) {
            val editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit()
            editor.putString("shopID", shopID)
            editor.apply()
        }

        /**
         * Get amount from SharedPreferences
         */
        fun getShopID(context: Context): String {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            try {
                return prefs.getString("shopID", "")
            } catch (e: Exception) {
                return "0"
            }
        }

        /**
         * Store key on SharedPreferences
         */
        fun storeCertificate(key: String, context: Context) {
            val editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit()
            editor.putString("key", key)
            editor.apply()
        }

        /**
         * Get amount from SharedPreferences
         */
        fun getCertificate(context: Context): String {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            try {
                return prefs.getString("key", "")
            } catch (e: Exception) {
                return "0"
            }

        }

    }

}



