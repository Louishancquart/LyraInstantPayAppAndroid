package com.lyranetwork.demo.payapp.Activity

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import com.lyranetwork.demo.payapp.R
import java.util.*


class NumberTextWatcher(private val editText: EditText, private val formatType: String) : TextWatcher {

    val T = "PayTextWatcher"

    private var current = ""
    private var insertingSelected = false
    private var isDeleting: Boolean = false


    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        Log.i(T, "::beforeTextChanged:" + "CharSequence " + s + " start=" + start + " count=" + count + " after=" +
                after)
        isDeleting = after <= 0 && count > 0
        if (s.toString() != current) {
            editText.removeTextChangedListener(this)
            val clean_text = s.toString().replace("[^\\d]".toRegex(), "")
            editText.setText(clean_text)
            editText.addTextChangedListener(this)
        }
    }


    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        Log.i(T, "::onTextChanged:" + "CharSequence " + s + " start=" + start + " count=" + count + " before=" +
                before)
        if (start == 0 && before >= 4) {
            insertingSelected = true
        }
    }


    @Synchronized override fun afterTextChanged(s: Editable) {
        Log.i(T, "::afterTextChanged:Editable $s; Current $current")
        if (s.toString() != current) {
            editText.removeTextChangedListener(this)
            val formatted_text: String = fromStringToFormattedString(s, formatType, insertingSelected)
            current = formatted_text
            editText.setText(formatted_text)
            editText.setSelection(formatted_text.length)
            editText.addTextChangedListener(this)
        }

        //Store values
        updateAmountEditTextState()
        MainActivity.Companion.storeAmount(editText.text.toString(), editText.context)
    }


    companion object {

        private val my_max_length = 9999

        fun editTextValueAmountToInt(editText: EditText): Int {
            return editText.text.toString().replace(",", "").replace(".", "").toInt()
        }

        fun fromStringToFormattedString(s: Editable, formatType: String,
                                        insertingSelected: Boolean): String {
            var digits = s.toString() //.replace(",", "").replace(".", "").toDouble()

            if (insertingSelected) {
                digits = toDouble(digits).toString()
            }
            var formatted_text: String
            try {
                formatted_text = String.format(Locale("en", "IN"), formatType, digits.toFloat())


            } catch (nfe: NumberFormatException) {
                var v_value = 0.0
//                v_value = toDouble(digits)
                formatted_text = String.format(Locale("en", "IN"), formatType, v_value)
            }
            return formatted_text
        }

        /**
         * @param str String with special caracters
         *
         * @return a double value of string
         */
        fun toDouble(str: String?): Double {
            var str = str
            str = str!!.replace("[^\\d]".toRegex(), "")
            if (str != null && str.length > 0) {

                var value = java.lang.Double.parseDouble(str)
                val s_value = java.lang.Double.toString(Math.abs(value / 100))
                val integerPlaces = s_value.indexOf('.')
                if (integerPlaces > my_max_length) {
                    value = java.lang.Double.parseDouble(deleteLastChar(str))
                }

                return value / 100
            } else {
                return 0.0
            }
        }

        private fun deleteLastChar(clean_text: String): String {
            var clean_text = clean_text
            if (clean_text.length > 0) {
                clean_text = clean_text.substring(0, clean_text.length - 1)
            } else {
                clean_text = "0"
            }
            return clean_text
        }
    }

    /**
     * Display error on amount editText if value not valid
     */
    private fun updateAmountEditTextState() {
        val currentAmount = editTextValueAmountToInt(editText)
        if (currentAmount != null && currentAmount > 5099) {
            editText.error = editText.context.resources.getString(R.string.invalid_amount)
        }
    }
}