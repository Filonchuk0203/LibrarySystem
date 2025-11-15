package com.example.myapplication.terminal

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.myapplication.R

data class BookItem(
    val title: String,
    var quantity: Int = 1
)

class BookListAdapter(
    private val context: Context,
    private val books: MutableList<BookItem>
) : BaseAdapter() {

    override fun getCount(): Int = books.size
    override fun getItem(position: Int): Any = books[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_book, parent, false)

        val book = books[position]

        val textBookInfo = view.findViewById<TextView>(R.id.textBookInfo)
        val buttonDecrease = view.findViewById<Button>(R.id.buttonDecrease)
        val buttonIncrease = view.findViewById<Button>(R.id.buttonIncrease)
        val textQuantity = view.findViewById<TextView>(R.id.textQuantity)

        textBookInfo.text = book.title
        textQuantity.text = book.quantity.toString()

        // 🔹 Зменшення кількості або видалення при 1 → 0
        buttonDecrease.setOnClickListener {
            if (book.quantity > 1) {
                book.quantity--
                textQuantity.text = book.quantity.toString()
            } else {
                // Якщо кількість = 1 → видаляємо книгу
                books.removeAt(position)
                notifyDataSetChanged()
            }
        }

        // 🔹 Збільшення кількості
        buttonIncrease.setOnClickListener {
            book.quantity++
            textQuantity.text = book.quantity.toString()
        }

        return view
    }

    fun getBooks(): MutableList<BookItem> = books

    fun addOrIncrementBook(bookTitle: String) {
        val existing = books.find { it.title == bookTitle }
        if (existing != null) {
            existing.quantity++
        } else {
            books.add(BookItem(bookTitle))
        }
        notifyDataSetChanged()
    }
}
