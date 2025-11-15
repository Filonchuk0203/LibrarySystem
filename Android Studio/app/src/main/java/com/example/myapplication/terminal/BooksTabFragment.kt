package com.example.myapplication.terminal

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myapplication.HttpClient
import com.example.myapplication.R
import org.json.JSONArray

class BooksTabFragment : Fragment() {

    private var tabType: String? = null
    private var libraryId: String? = null
    private var clientId: String? = null
    private var password: String? = null
    private var librarianId: String? = null

    private lateinit var httpClient: HttpClient
    private lateinit var url: String
    private lateinit var listView: ListView
    private lateinit var buttonAdd: Button
    private lateinit var buttonAction: Button
    private lateinit var autoComplete: AutoCompleteTextView
    private lateinit var spinnerFilter: Spinner
    private lateinit var bookAdapter: BookListAdapter

    private var allBooks: MutableList<String> = mutableListOf()
    private var currentBooks: MutableList<String> = mutableListOf()
    private var currentFilter: String = "Назва"

    companion object {
        fun newInstance(
            type: String,
            libraryId: String?,
            clientId: String?,
            password: String?,
            librarianId: String?
        ): BooksTabFragment {
            val fragment = BooksTabFragment()
            val args = Bundle().apply {
                putString("type", type)
                putString("libraryId", libraryId)
                putString("clientId", clientId)
                putString("password", password)
                putString("librarianId", librarianId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tabType = arguments?.getString("type")
        libraryId = arguments?.getString("libraryId")
        clientId = arguments?.getString("clientId")
        password = arguments?.getString("password")
        librarianId = arguments?.getString("librarianId")

        // 🔹 Ініціалізуємо httpClient і url
        httpClient = HttpClient()
        url = requireContext().getString(R.string.server_url)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_books_tab, container, false)

        listView = view.findViewById(R.id.listView)
        bookAdapter = BookListAdapter(requireContext(), mutableListOf())
        listView.adapter = bookAdapter
        buttonAdd = view.findViewById(R.id.buttonAddBook)
        buttonAction = view.findViewById(R.id.buttonAction)
        autoComplete = view.findViewById(R.id.autoCompleteTextViewFind)
        spinnerFilter = view.findViewById(R.id.spinnerFilter)

        setupSpinner()
        setupButtons()

        return view
    }

    private var isSpinnerInitialized = false

    private fun setupSpinner() {
        val filterOptions = listOf("Назва", "ISBN")

        val spinnerAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filterOptions
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                view.setPadding(view.paddingLeft, 20, view.paddingRight, 20)
                return view
            }
        }

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = spinnerAdapter
        spinnerFilter.setSelection(0)
        currentFilter = filterOptions[0]

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentFilter = filterOptions[position]

                if (isSpinnerInitialized) {
                    loadBooksForTab()
                } else {
                    isSpinnerInitialized = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupButtons() {
        when (tabType) {
            "purchase" -> {
                buttonAdd.text = "Додати книгу для купівлі"
                buttonAction.text = "Купити книги"
            }
            "issue" -> {
                buttonAdd.text = "Додати книгу для видачі"
                buttonAction.text = "Видати книги"
            }
            "return" -> {
                buttonAdd.text = "Додати книгу для повернення"
                buttonAction.text = "Повернути книги"
            }
        }

        buttonAdd.setOnClickListener {
            val inputText = autoComplete.text.toString().trim()
            if (inputText.isEmpty()) {
                Toast.makeText(requireContext(), "Введіть назву або ISBN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addBookToList(inputText)
            autoComplete.setText("")
        }

        buttonAction.setOnClickListener {
            val books = bookAdapter.getBooks()
            if (books.isEmpty()) {
                Toast.makeText(requireContext(), "Список доданих книг порожній", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val totalQuantity = books.sumOf { it.quantity }
            val booksInfo = books.joinToString(separator = "\n") { book ->
                val titlePart = book.title
                val quantityPart = if (book.quantity > 1) " : ${book.quantity} примірники" else ""
                "$titlePart$quantityPart"
            }

            val actionTitle: String
            val confirmButton: String
            val functionName: String

            when (tabType) {
                "purchase" -> {
                    actionTitle = "Купівля книг"
                    confirmButton = "Оплатити"
                    functionName = "terminal_purchase_books"
                }
                "issue" -> {
                    actionTitle = "Підтвердження видачі"
                    confirmButton = "Видати"
                    functionName = "terminal_issue_books"
                }
                "return" -> {
                    actionTitle = "Підтвердження повернення"
                    confirmButton = "Повернути"
                    functionName = "terminal_return_books"
                }
                else -> return@setOnClickListener
            }

            // 🔹 Формуємо один запит з масивами ISBN та кількостей
            val listIsbn = books.map { it.title.substringAfter("(").removeSuffix(")").trim() }
            val listQuantity = books.map { it.quantity }

            val json = """
                {
                    "function_name": "$functionName",
                    "param_dict": {
                        "library_id": "$libraryId",
                        "client_id": "$clientId",
                        "librarian_id": "$librarianId",
                        "list_isbn": ${listIsbn.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                        "list_quantity": ${listQuantity.joinToString(prefix = "[", postfix = "]")}
                    }
                }
            """.trimIndent()

            // 🔹 Діалог підтвердження перед виконанням
            AlertDialog.Builder(requireContext())
                .setTitle(actionTitle)
                .setMessage("Загальна кількість: $totalQuantity\n\n$booksInfo\n\nВиконати дію?")
                .setNegativeButton("Скасувати", null)
                .setPositiveButton(confirmButton) { _, _ ->
                    val act = requireActivity()

                    httpClient.safePostRequest(act, url, json) { jsonResponse ->
                        val resultValue = jsonResponse.opt("result")

                        act.runOnUiThread {
                            if (resultValue is String) {
                                Toast.makeText(requireContext(), resultValue, Toast.LENGTH_SHORT).show()
                                if (resultValue == "Операція успішна") {
                                    bookAdapter.getBooks().clear()
                                    bookAdapter.notifyDataSetChanged()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Помилка в запиті до серверу", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .show()
        }

    }

    /** Завантаження списку книг залежно від типу вкладки **/
    private fun loadBooksForTab() {
        val act = activity ?: return

        if (libraryId.isNullOrEmpty()) {
            act.runOnUiThread {
                Toast.makeText(act, "Не вдалося знайти ID бібліотеки", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val functionName = when (tabType) {
            "return" -> "terminal_get_mybooks"
            else -> "terminal_get_books"
        }

        val extraParam = if (tabType == "return" && !clientId.isNullOrEmpty()) ""","client_id": "$clientId"""" else ""
        val json = """{
            "function_name": "$functionName",
            "param_dict": {
                "library_id": "$libraryId"$extraParam
            }
        }"""

        httpClient.safePostRequest(act, url, json) { jsonResponse ->
            val result = jsonResponse["result"]
            if (result is JSONArray) {
                allBooks.clear()
                for (i in 0 until result.length()) {
                    val book = result.getJSONArray(i)
                    val title = book.optString(0)
                    val author = book.optString(1)
                    val isbn = book.optString(2)
                    allBooks.add("$title - $author ($isbn)")
                }
                act.runOnUiThread {
                    setupAutoComplete()
                }
            } else {
                act.runOnUiThread {
                    Toast.makeText(act, "Помилка у відповіді сервера при завантаженні книг", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupAutoComplete() {
        val data = when (currentFilter) {
            "Назва" -> allBooks.map { it.substringBefore("(").trim() }
            "ISBN" -> allBooks.map { it.substringAfter("(").removeSuffix(")").trim() }
            else -> allBooks
        }.distinct()

        val autoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, data)
        autoComplete.setAdapter(autoAdapter)
    }

    /** Додає книгу в список для дії (купівлі/видачі/повернення) **/
    private fun addBookToList(bookValue: String) {
        val book = allBooks.find { it.contains(bookValue, ignoreCase = true) }
        if (book == null) {
            Toast.makeText(requireContext(), "Книгу не знайдено в базі", Toast.LENGTH_SHORT).show()
            return
        }

        bookAdapter.addOrIncrementBook(book)
    }

    private var dataLoaded = false

    fun onTabVisible() {
        if (!dataLoaded) {
            dataLoaded = true
            loadBooksForTab()
        }
    }
}
