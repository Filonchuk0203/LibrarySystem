package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class LibraryRegistrationFragment : Fragment() {
    private lateinit var editTextLibraryName: EditText
    private lateinit var editTextLibraryAddress: EditText
    private lateinit var editTextLibrarySecretPassword: EditText
    private lateinit var httpClient: HttpClient
    private lateinit var btnRegisterLibrary: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.library_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextLibraryName = view.findViewById(R.id.editTextLibraryName)
        editTextLibraryAddress = view.findViewById(R.id.editTextLibraryAddress)
        editTextLibrarySecretPassword = view.findViewById(R.id.editTextLibrarySecretPassword)
        httpClient = HttpClient()

        btnRegisterLibrary = view.findViewById(R.id.btnRegisterLibrary)
        btnRegisterLibrary.setOnClickListener {
            if (areLibraryFieldsEmpty(
                    editTextLibraryName,
                    editTextLibraryAddress,
                    editTextLibrarySecretPassword
                )
            ) {
                Toast.makeText(requireContext(), "Помилка: Будь ласка, заповніть всі поля!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val libraryName = editTextLibraryName.text.toString()
            val address = editTextLibraryAddress.text.toString()
            val secretPassword = editTextLibrarySecretPassword.text.toString()

            val url = getString(R.string.server_url)
            val json = """{
                "function_name": "add_library",
                "param_dict": {
                    "library_name": "$libraryName",
                    "address": "$address",
                    "systempassword": "$secretPassword"
                }
            }"""

            httpClient.safePostRequest(requireActivity(), url, json) { jsonResponse ->
                val number = jsonResponse.getInt("result")
                if (number != 0 && number != -1) {
                    Toast.makeText(
                        requireContext(),
                        "Бібліотека створена. Зареєструйте для неї нового бібліотекаря.",
                        Toast.LENGTH_SHORT
                    ).show()
                    requireActivity().finish()
                } else if (number == -1) {
                    Toast.makeText(
                        requireContext(),
                        "Помилка: Бібліотека з даним ім'ям вже існує!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Помилка в запиті до серверу",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun areLibraryFieldsEmpty(
        libraryNameEditText: EditText,
        addressEditText: EditText,
        secretPasswordEditText: EditText
    ): Boolean {
        val editTexts = arrayOf(libraryNameEditText, addressEditText, secretPasswordEditText)
        for (editText in editTexts) {
            if (editText.text.toString().isEmpty()) {
                return true
            }
        }
        return false
    }
}
