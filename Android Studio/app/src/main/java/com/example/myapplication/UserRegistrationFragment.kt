package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject

class UserRegistrationFragment : Fragment() {
    private lateinit var editTextName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextSurname: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextLogin: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextRepeatPassword: EditText
    private lateinit var autoCompleteTextViewLibrary: AutoCompleteTextView
    private lateinit var editTextSystemPassword: EditText
    private lateinit var httpClient: HttpClient
    private lateinit var buttonRegister: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.user_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextName = view.findViewById(R.id.editTextName)
        editTextLastName = view.findViewById(R.id.editTextLastName)
        editTextSurname = view.findViewById(R.id.editTextSurname)
        editTextPhone = view.findViewById(R.id.editTextPhone)
        editTextAddress = view.findViewById(R.id.editTextAddress)
        editTextEmail = view.findViewById(R.id.editTextEmail)
        editTextLogin = view.findViewById(R.id.editTextLogin)
        editTextPassword = view.findViewById(R.id.editTextPassword)
        editTextRepeatPassword = view.findViewById(R.id.editTextRepeatPassword)
        autoCompleteTextViewLibrary = view.findViewById(R.id.autoCompleteTextViewLibrary)
        editTextSystemPassword = view.findViewById(R.id.editTextSystemPassword)
        httpClient = HttpClient()

        val url = getString(R.string.server_url)

        // Отримання бібліотек
        var json = """{
            "function_name": "get_name_libraries"
        }"""

        httpClient.safePostRequest(requireActivity(), url, json) { jsonResponse ->
            val resultValue = jsonResponse["result"]
            if (resultValue is JSONArray) {
                val allLibraries = mutableListOf<String>()
                for (i in 0 until resultValue.length()) {
                    val libraryName = resultValue.getString(i)
                    allLibraries.add(libraryName)
                }
                val libraryAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    allLibraries
                )
                autoCompleteTextViewLibrary.setAdapter(libraryAdapter)
            } else {
                Toast.makeText(requireContext(), "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
            }
        }

        // Реєстрація користувача
        buttonRegister = view.findViewById(R.id.btnRegister)
        buttonRegister.setOnClickListener {
            val password = editTextRepeatPassword.text.toString()
            val repeatPassword = editTextPassword.text.toString()
            if (password == repeatPassword && password.length > 7) {
                if (areLibrarianFieldsEmpty(
                        editTextName,
                        editTextLastName,
                        editTextPhone,
                        editTextAddress,
                        editTextLogin
                    )
                ) {
                    Toast.makeText(requireContext(), "Помилка: Будь ласка, заповніть всі поля", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val name = editTextName.text.toString()
                val lastName = editTextLastName.text.toString()
                val surName = editTextSurname.text.toString()
                val phone = editTextPhone.text.toString()
                val address = editTextAddress.text.toString()
                val email = editTextEmail.text.toString()
                val login = editTextLogin.text.toString()
                val libraryName = autoCompleteTextViewLibrary.text.toString()
                val systemPassword = editTextSystemPassword.text.toString()

                val json = """{
                    "function_name": "add_librarian",
                    "param_dict": {
                        "last_name": "$lastName",
                        "user_name": "$name",
                        "sur_name": "$surName",
                        "address": "$address",
                        "phone": "$phone",
                        "email": "$email",
                        "login": "$login",
                        "password": "$password",
                        "library_name": "$libraryName",
                        "system_password": "$systemPassword"
                    }
                }"""

                httpClient.safePostRequest(requireActivity(), url, json) { jsonResponse ->
                    val resultValue = jsonResponse["result"]
                    when {
                        resultValue is JSONArray -> {
                            val intent = Intent(requireContext(), MainMenu::class.java)
                            intent.putExtra("librarianId", resultValue.getString(0))
                            intent.putExtra("libraryId", resultValue.getString(1))
                            intent.putExtra("password", password)
                            Toast.makeText(requireContext(), "Реєстрація успішна", Toast.LENGTH_SHORT).show()
                            requireActivity().startActivity(intent)
                            requireActivity().finish()
                        }
                        resultValue == -2 -> Toast.makeText(
                            requireContext(),
                            "Помилка: Введеної бібліотеки не існує або невірний системний пароль",
                            Toast.LENGTH_SHORT
                        ).show()
                        resultValue == -1 -> Toast.makeText(
                            requireContext(),
                            "Даний логін вже існує. Введіть інший.",
                            Toast.LENGTH_SHORT
                        ).show()
                        else -> Toast.makeText(
                            requireContext(),
                            "Помилка в запиті до серверу",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Помилка: Пароль замалий (Менше 8 символів) або повторений неправильно", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun areLibrarianFieldsEmpty(
        nameEditText: EditText,
        lastNameEditText: EditText,
        phoneEditText: EditText,
        addressEditText: EditText,
        loginEditText: EditText
    ): Boolean {
        val editTexts = arrayOf(nameEditText, lastNameEditText, phoneEditText, addressEditText, loginEditText)
        for (editText in editTexts) {
            if (editText.text.toString().isEmpty()) {
                return true
            }
        }
        return false
    }
}
