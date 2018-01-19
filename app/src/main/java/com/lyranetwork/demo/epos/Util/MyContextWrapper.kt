package com.lyranetwork.demo.epos.Util

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.*


@Suppress("DEPRECATION")
/**
 * Used to force the locale
 */
class MyContextWrapper(base: Context) : ContextWrapper(base) {
    companion object {

        fun wrap(context: Context, language: String): ContextWrapper {
            var myContext = context
            val config = myContext.getResources().getConfiguration()
            var sysLocale: Locale?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sysLocale = getSystemLocale(config)
            } else {
                sysLocale = getSystemLocaleLegacy(config)
            }
            if (language != "" && !sysLocale.getLanguage().equals(language)) {
                val locale = Locale(language)
                Locale.setDefault(locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setSystemLocale(config, locale)
                } else {
                    setSystemLocaleLegacy(config, locale)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    myContext = myContext.createConfigurationContext(config)
                } else {
                    @Suppress("DEPRECATION")
                    myContext.getResources().updateConfiguration(config, myContext.getResources().getDisplayMetrics())
                }
            }
            return MyContextWrapper(myContext)
        }

        fun getSystemLocaleLegacy(config: Configuration): Locale {
            return config.locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        fun getSystemLocale(config: Configuration): Locale {
            return config.getLocales().get(0)
        }

        fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
            config.locale = locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        fun setSystemLocale(config: Configuration, locale: Locale) {
            config.setLocale(locale)
        }
    }
}