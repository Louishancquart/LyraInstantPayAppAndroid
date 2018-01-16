package com.lyranetwork.demo.payapp.Activity

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.lyranetwork.demo.payapp.BuildConfig
import com.lyranetwork.demo.payapp.R
import com.lyranetwork.demo.payapp.Util.MyContextWrapper
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


private var MY_PREFS_NAME = "PayApp"

/**
 * MainActivity, contains payment form and language selection
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Listeners
        initListener()

        //get credentials if it exist
        checkForCredentials()

        // Update "Powered by" with version
        textViewPoweredBy.setText(resources.getString(R.string.powered_by_lyra) + " v" + BuildConfig.VERSION_NAME)

        // Set status bar color
        setStatusBarColor()

        //Init EditText with stored value1000s
        initEditText()

        //Update the flag with the selected language
        updateFlagWithCurrentLang()

        //Default mode
        //mode()

        //Force progress bar color
        progressBar.getIndeterminateDrawable().setColorFilter(resources.getColor(R.color.bluelyradark),
                                                              PorterDuff.Mode.MULTIPLY)
    }

    @Suppress("DEPRECATION")
    private fun updateFlagWithCurrentLang() {
        if (Locale.getDefault().language.equals(LanguagesEnum.identifier(LanguagesEnum.ENGLISH))) {
            imageButtonLang.setImageDrawable(resources.getDrawable(R.drawable.unitedkingdom))
        } else if (Locale.getDefault().language.equals(LanguagesEnum.identifier(LanguagesEnum.FRENCH))) {
            imageButtonLang.setImageDrawable(resources.getDrawable(R.drawable.france))
        } else if (Locale.getDefault().language.equals(LanguagesEnum.identifier(LanguagesEnum.SPANISH))) {
            imageButtonLang.setImageDrawable(resources.getDrawable(R.drawable.spain))
        }
    }



    /**
     * Init EditText with stored values
     */
    private fun initEditText() {
        editTextOrderID.setText(getEmail(applicationContext), TextView.BufferType.NORMAL)
        val amount: String = getAmount(applicationContext)
        editTextAmount.setText(amount, TextView.BufferType.NORMAL)
    }

    /**
     * Init Credential with stored values
     */
    private fun checkForCredentials() {
        val shopID = CredentialsActivity.getShopID(applicationContext)
        val certificate = CredentialsActivity.getCertificate(applicationContext)

        println("shopID:" + shopID)

        if( shopID.isBlank() || certificate.isBlank() ){
        //go to credentials activity
         val intent = Intent(this, CredentialsActivity::class.java)
            startActivity(intent)
        }
    }




    /**
     * Listeners
     */
    @Suppress("DEPRECATION")
    private fun initListener() {
        //Init EditText amount
        val numberTextWatcher = NumberTextWatcher(editTextAmount, "%,.2f")
        editTextAmount.addTextChangedListener(numberTextWatcher)
        editTextAmount.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(p0: View?, hasFocus: Boolean) {
                if (hasFocus) editTextAmount.setSelection(editTextAmount.getText().length)
            }
        })
        editTextAmount.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                editTextAmount.setSelection(editTextAmount.getText().length)
            }
        })


        //Init EditText orderID
        editTextOrderID?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                updateOrderIDEditTextState()
                storeOrderID(editTextOrderID.text.toString(), applicationContext)
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        //Init Button pay
        paybutton?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (isPaymentValid()) {

                    val orderID = editTextOrderID.text.toString()
                    val amount = editTextAmount.text.toString().replace(",", "").replace(".", "").toInt()

                    val lang = MainActivity.getLang(applicationContext).toString()

                    // Go to payment (WebviewActivity)
                    loadingPanel.visibility = View.VISIBLE
//                    openWebViewActivity(orderID, amount, lang)
                    openPaymentLinkReceivedActivity(orderID, amount, lang)

                } else {
                    if (!isAmountValid()) {
                        if (editTextAmount.text.toString().equals("0,00")) editTextAmount.error = resources.getString(
                                R.string.invalid_input)
                        else editTextAmount.error = resources.getString(R.string.invalid_amount)
                    }
                    if (!isOrderValid()) {
                        if (TextUtils.isEmpty(editTextOrderID?.text)) editTextOrderID.error = resources.getString(
                                R.string.invalid_input)
                        else editTextOrderID.error = resources.getString(R.string.invalid_order)
                    }
                }
            }
        })

        //Init Button param
        imageButtonLang?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val items = arrayOf<CharSequence>(
                        Html.fromHtml("<font color='#3775ba'>" + getString(R.string.english) + "</font>"),
                        Html.fromHtml("<font color='#3775ba'>" + getString(R.string.french) + "</font>"),
                        Html.fromHtml("<font color='#3775ba'>" + getString(R.string.spanish) + "</font>"))
                val builder = AlertDialog.Builder(this@MainActivity)
                var title = TextView(this@MainActivity)
                title.setText(Html.fromHtml("<font color='#3775ba'>" + getString(R.string.language) + "</font>"))
                title.setPadding(10, 35, 10, 10)
                title.setGravity(Gravity.CENTER)
                title.setTextSize(24.0F)
                builder.setCustomTitle(title)
                builder.setItems(items) { _, item ->
                    when (item) {
                        0 -> if (!Locale.getDefault().language.equals(
                                LanguagesEnum.identifier(LanguagesEnum.ENGLISH))) {
                            storeLang(applicationContext, LanguagesEnum.ENGLISH);reloadMainActivity()
                        }
                        1 -> if (!Locale.getDefault().language.equals(LanguagesEnum.identifier(LanguagesEnum.FRENCH))) {
                            storeLang(applicationContext, LanguagesEnum.FRENCH);reloadMainActivity()
                        }
                        2 -> if (!Locale.getDefault().language.equals(
                                LanguagesEnum.identifier(LanguagesEnum.SPANISH))) {
                            storeLang(applicationContext, LanguagesEnum.SPANISH);reloadMainActivity()
                        }
                    }
                }
                builder.create().show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadingPanel.visibility = View.GONE
//        storeAmount("0,00", applicationContext)
//        storeOrderID("", applicationContext)
        editTextAmount.setText(getAmount(applicationContext))
        editTextOrderID.setText(getOrderID(applicationContext))



    }



    override fun onDestroy() {
        super.onDestroy()
        // Erase values on destroy
        storeAmount("0,00", applicationContext)
        storeOrderID("", applicationContext)
        if (isFinishing()) {
            //call some method
        }
    }

    override fun onPause() {
        super.onPause()
        // Store values onPause (values retrieved after language switched)
//        storeAmount(editTextAmount.text.toString(), applicationContext)
//        storeOrderID(editTextOrderID.text.toString(), applicationContext)
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(
                        "<font color='#293c7a'>" + resources.getString(R.string.areyousureexit) + "</font>"))
                .setCancelable(false)
                .setPositiveButton(
                        Html.fromHtml("<font color='#293c7a'>" + resources.getString(R.string.yes) + "</font>"),
                        object : DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                MainActivity.Companion.storeLang(applicationContext, null)
                                MainActivity.setLanguageForApp(
                                        MainActivity.LanguagesEnum.identifier(MainActivity.getLang(applicationContext)),
                                        applicationContext)
                                finish()
                            }
                        })
                .setNegativeButton(
                        Html.fromHtml("<font color='#293c7a'>" + resources.getString(R.string.no) + "</font>"), null)
                .show()
    }

    /**
     * Display error on orderID editText if value not valid
     */
    private fun updateOrderIDEditTextState() {
        if (TextUtils.isEmpty(editTextOrderID.text) ) {
            editTextOrderID.error = resources.getString(R.string.invalid_order)
        }
    }

    /**
     * Ready to make a payment?
     */
    private fun isPaymentValid(): Boolean {
        return isAmountValid()
    }

    /**
     * Is orderID valid (compare with a regular expression)
     */
    private fun isOrderValid(): Boolean {
        return !TextUtils.isEmpty(editTextOrderID?.text)
    }

    /**
     * Is amount valid (amount < 5000)
     */
    private fun isAmountValid(): Boolean {
        val currentAmount: Int? = NumberTextWatcher.Companion.editTextValueAmountToInt(editTextAmount)
        return currentAmount != null && currentAmount > 0 && currentAmount <= 500000
    }

    /**
     * Open WebviewActivity (contains a WebView)
     */
    private fun openWebViewActivity(orderID: String, amount: Int, lang: String) {
        val intent = Intent(this, WebviewActivity::class.java)
        intent.putExtra("orderID", orderID)
        intent.putExtra("amount", amount)
        startActivity(intent)
    }

        /**
         * Open WebviewActivity (contains a WebView)
         */
        private fun openPaymentLinkReceivedActivity(orderID: String, amount: Int, lang: String) {
            val intent = Intent(this, PaymentLinkReceivedActivity::class.java)
            intent.putExtra("orderID", orderID)
            intent.putExtra("amount", amount/100)
            startActivity(intent)
        }

    /**
     * Set status bar color to match with the background
     */
    private fun setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = this.getWindow()
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            // finally change the color
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar))
        }
    }

    /**
     * Force selected language
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase, LanguagesEnum.Companion.identifier(getLang(newBase))))
        setLanguageForApp(LanguagesEnum.Companion.identifier(getLang(newBase)), newBase)
    }

    /**
     * Reload MainActivity to update language
     */
    private fun reloadMainActivity() {
        val refresh = Intent(this@MainActivity, MainActivity::class.java)
        refresh.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptionsCompat.makeCustomAnimation(applicationContext, android.R.anim.fade_in,
                android.R.anim.fade_out)
        ActivityCompat.startActivity(this@MainActivity, refresh, options.toBundle())



        finish()
    }

    companion object {

        /**
         * Store language on SharedPreferences
         */
        fun storeLang(context: Context, lang: LanguagesEnum?) {
            val editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (lang == null) {
                editor.remove("lang")
            } else {
                editor.putString("lang", lang.toString())
            }
            editor.apply()
        }

        /**
         * Get language from SharedPreferences
         * Default value: English
         */
        fun getLang(context: Context): LanguagesEnum {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            var restoredLang = prefs.getString("lang", null)
            if (restoredLang == null) {
                when (Resources.getSystem().getConfiguration().locale.country.toLowerCase()) {
                    "fr" -> restoredLang = LanguagesEnum.FRENCH.toString()
                    "es" -> restoredLang = LanguagesEnum.SPANISH.toString()
                    else -> restoredLang = LanguagesEnum.ENGLISH.toString()
                }
            }
            return LanguagesEnum.valueOf(restoredLang)
        }

        /**
         * Set language for App
         * language : language to apply
         */
        @Suppress("DEPRECATION")
        fun setLanguageForApp(language: String, context: Context) {
            val locale: Locale
            if (language == "not-set") {
                locale = Locale.getDefault()
            } else {
                locale = Locale(language)
            }
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics())
        }

        /**
         * Store orderID on SharedPreferences
         */
        fun storeOrderID(orderID: String, context: Context) {
            val editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit()
            editor.putString("orderID", orderID)
            editor.apply()
        }

        /**
         * Store orderID on SharedPreferences
         */
        fun getOrderID(context: Context): String {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            try {
                return prefs.getString("orderID", "")
            }catch(e: Exception){
                return "0"
            }

        }

        /**
         * Get orderID from SharedPreferences
         */
        fun getEmail(context: Context): String {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            val restoredEmail = prefs.getString("orderID", "")
            if (restoredEmail != null) {
                return restoredEmail
            } else {
                return ""
            }
        }

        /**
         * Store amount on SharedPreferences
         */
        fun storeAmount(amount: String, context: Context) {
            val editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit()
            editor.putString("amount", amount)
            editor.apply()
        }

        /**
         * Get amount from SharedPreferences
         */
        fun getAmount(context: Context): String {
            val prefs = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE)
            try {
                return prefs.getString("amount", "0,00")
            }catch(e: Exception){
                return "0"
            }

        }


    }

    /**
     * Enum languages
     */
    enum class LanguagesEnum {
        ENGLISH, FRENCH, SPANISH;

        companion object {
            /**
             * Convert an enum language to a locale with 2 characters
             */
            fun identifier(languagesEnum: LanguagesEnum): String {
                return when (languagesEnum) {
                    ENGLISH -> "en"
                    FRENCH -> "fr"
                    SPANISH -> "es"
                }
            }
        }
    }

}
