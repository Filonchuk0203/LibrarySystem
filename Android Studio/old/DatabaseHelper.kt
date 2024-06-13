package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 3
        private const val DATABASE_NAME = "YourDatabaseName"
    }

    override fun onCreate(db: SQLiteDatabase) {

        val bookTable = """
        CREATE TABLE IF NOT EXISTS Book (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT,
            author TEXT,
            publicationYear INTEGER,
            isbn TEXT,
            genre TEXT,
            pageCount INTEGER,
            availableCopies INTEGER,
            publisher TEXT
        )
        """

        val readerTable = """
        CREATE TABLE IF NOT EXISTS Reader (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            lastName TEXT,
            firstName TEXT,
            middleName TEXT,
            address TEXT,
            phone TEXT,
            email TEXT,
            takenBooks INTEGER
        )
        """

        val librarianTable = """
        CREATE TABLE IF NOT EXISTS Librarian (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            lastName TEXT,
            firstName TEXT,
            middleName TEXT,
            address TEXT,
            phone TEXT,
            email TEXT,
            login TEXT,
            password TEXT
        )
        """

        val issueTable = """
        CREATE TABLE IF NOT EXISTS Issue (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            issueDate TEXT,
            returnDate TEXT,
            status TEXT,
            book_id INTEGER REFERENCES Book(id),
            reader_id INTEGER REFERENCES Reader(id),
            librarian_id INTEGER REFERENCES Librarian(id)
        )
        """

        db.execSQL(bookTable)
        db.execSQL(readerTable)
        db.execSQL(librarianTable)
        db.execSQL(issueTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    @SuppressLint("Range")
    fun checkLibrarianCredentials(login: String, password: String): Int {
        val db = readableDatabase
        val columns = arrayOf("id")
        val selection = "login = ? AND password = ?"
        val selectionArgs = arrayOf(login, password)

        val cursor = db.query("Librarian", columns, selection, selectionArgs, null, null, null)

        var librarianId = -1
        if (cursor.moveToFirst()) {
            librarianId = cursor.getInt(cursor.getColumnIndex("id"))
        }

        cursor.close()

        return librarianId
    }

    fun addLibrarian(name: String, lastName: String, surName: String, phone: String,
                     address: String, email: String, login: String, password: String): Int {
        if (librarianExists(login)) {
            return -1
        }

        val db = this.writableDatabase
        val values = ContentValues()

        values.put("lastName", lastName)
        values.put("firstName", name)
        values.put("middleName", surName)
        values.put("address", address)
        values.put("phone", phone)
        values.put("email", email)
        values.put("login", login)
        values.put("password", password)

        val id = db.insert("Librarian", null, values).toInt()
        db.close()

        return id
    }

    private fun librarianExists(login: String): Boolean {
        val db = readableDatabase
        val query = "SELECT id FROM Librarian WHERE login = ?"
        val cursor = db.rawQuery(query, arrayOf(login))
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }

    @SuppressLint("Range")
    fun getAllLibrarians(): ArrayList<String> {
        val librariansList = ArrayList<String>()
        val db = this.readableDatabase
        val query = "SELECT * FROM Librarian"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val lastName = cursor.getString(cursor.getColumnIndex("lastName"))
            val firstName = cursor.getString(cursor.getColumnIndex("firstName"))
            val middleName = cursor.getString(cursor.getColumnIndex("middleName"))
            val address = cursor.getString(cursor.getColumnIndex("address"))
            val phone = cursor.getString(cursor.getColumnIndex("phone"))
            val email = cursor.getString(cursor.getColumnIndex("email"))
            val login = cursor.getString(cursor.getColumnIndex("login"))

            val librarianInfo = "ID: $id\n" +
                    "Name: $lastName $firstName $middleName\n" +
                    "Address: $address\n" +
                    "Phone: $phone\n" +
                    "Email: $email\n" +
                    "Login: $login\n"
            librariansList.add(librarianInfo)
        }

        cursor.close()
        db.close()
        return librariansList
    }

    fun addBook(title: String, author: String, publicationYear: Int, isbn: String, genre: String,
                pageCount: Int, availableCopies: Int, publisher: String): Int {
        if (bookExists(isbn, "isbn")) {
            return -1
        }

        val db = this.writableDatabase
        val values = ContentValues()

        values.put("title", title)
        values.put("author", author)
        values.put("publicationYear", publicationYear)
        values.put("isbn", isbn)
        values.put("genre", genre)
        values.put("pageCount", pageCount)
        values.put("availableCopies", availableCopies)
        values.put("publisher", publisher)

        val id = db.insert("Book", null, values).toInt()
        db.close()

        return id
    }

    fun updateBook(id: String, title: String, author: String, publicationYear: Int, isbn: String, genre: String,
                   pageCount: Int, availableCopies: Int, publisher: String): Int {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put("title", title)
        values.put("author", author)
        values.put("publicationYear", publicationYear)
        values.put("isbn", isbn)
        values.put("genre", genre)
        values.put("pageCount", pageCount)
        values.put("availableCopies", availableCopies)
        values.put("publisher", publisher)

        val rowsAffected = db.update("Book", values, "id = ?", arrayOf(id))
        db.close()

        return rowsAffected
    }


    fun bookExists(param: String, key: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM Book WHERE $key = ?"
        val cursor = db.rawQuery(query, arrayOf(param))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    @SuppressLint("Range")
    fun getAllBooks(): ArrayList<String> {
        val booksList = ArrayList<String>()
        val db = this.readableDatabase
        val query = "SELECT * FROM Book"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val title = cursor.getString(cursor.getColumnIndex("title"))
            val author = cursor.getString(cursor.getColumnIndex("author"))
            val publicationYear = cursor.getInt(cursor.getColumnIndex("publicationYear"))
            val isbn = cursor.getString(cursor.getColumnIndex("isbn"))
            val genre = cursor.getString(cursor.getColumnIndex("genre"))
            val pageCount = cursor.getInt(cursor.getColumnIndex("pageCount"))
            val availableCopies = cursor.getInt(cursor.getColumnIndex("availableCopies"))
            val publisher = cursor.getString(cursor.getColumnIndex("publisher"))

            val bookInfo = "ID: $id\n" +
                    "Title: $title\n" +
                    "Author: $author\n" +
                    "Publication Year: $publicationYear\n" +
                    "ISBN: $isbn\n" +
                    "Genre: $genre\n" +
                    "Page Count: $pageCount\n" +
                    "Available Copies: $availableCopies\n" +
                    "Publisher: $publisher\n"

            booksList.add(bookInfo)
        }

        cursor.close()
        db.close()
        return booksList
    }

    fun deleteBook(bookISBN: String): Boolean {

        if (!bookExists(bookISBN, "id")) {
            return false
        }

        val db = this.writableDatabase
        try {
            val result = db.delete("Book", "id = ?", arrayOf(bookISBN))
            db.close()
            return result != -1
        } catch (e: Exception) {
            e.printStackTrace()
            db.close()
            return false
        }
    }

    @SuppressLint("Range")
    fun getNameBooks(): List<String> {
        val bookList = mutableListOf<String>()
        val db = this.readableDatabase

        val query = "SELECT title FROM Book"
        val cursor: Cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val title = cursor.getString(cursor.getColumnIndex("title"))
            bookList.add(title)
        }
        cursor.close()
        db.close()

        return bookList
    }


    fun addReader(lastName: String, firstName: String, middleName: String, address: String,
                  phone: String, email: String, takenBooks: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put("lastName", lastName)
        values.put("firstName", firstName)
        values.put("middleName", middleName)
        values.put("address", address)
        values.put("phone", phone)
        values.put("email", email)
        values.put("takenBooks", takenBooks)

        val id = db.insert("Reader", null, values).toInt()
        db.close()

        return id
    }

    fun updateReader(id: String, lastName: String, firstName: String, middleName: String, address: String,
                     phone: String, email: String): Int {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put("lastName", lastName)
        values.put("firstName", firstName)
        values.put("middleName", middleName)
        values.put("address", address)
        values.put("phone", phone)
        values.put("email", email)

        val rowsAffected = db.update("Reader", values, "id = ?", arrayOf(id))
        db.close()

        return rowsAffected
    }

    fun readerExists(param: String, key: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM Reader WHERE $key = ?"
        val cursor = db.rawQuery(query, arrayOf(param))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    @SuppressLint("Range")
    fun getAllReaders(): ArrayList<String> {
        val readersList = ArrayList<String>()
        val db = this.readableDatabase
        val query = "SELECT * FROM Reader"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val lastName = cursor.getString(cursor.getColumnIndex("lastName"))
            val firstName = cursor.getString(cursor.getColumnIndex("firstName"))
            val middleName = cursor.getString(cursor.getColumnIndex("middleName"))
            val address = cursor.getString(cursor.getColumnIndex("address"))
            val phone = cursor.getString(cursor.getColumnIndex("phone"))
            val email = cursor.getString(cursor.getColumnIndex("email"))
            val takenBooks = cursor.getInt(cursor.getColumnIndex("takenBooks"))

            val readerInfo = "ID: $id\n" +
                    "Last Name: $lastName\n" +
                    "First Name: $firstName\n" +
                    "Middle Name: $middleName\n" +
                    "Address: $address\n" +
                    "Phone: $phone\n" +
                    "Email: $email\n" +
                    "Taken Books: $takenBooks\n"

            readersList.add(readerInfo)
        }

        cursor.close()
        db.close()
        return readersList
    }

    fun deleteReader(readerId: String): Boolean {
        if (!readerExists(readerId, "id")) {
            return false
        }

        val db = this.writableDatabase
        try {
            val result = db.delete("Reader", "id = ?", arrayOf(readerId))
            db.close()
            return result != -1
        } catch (e: Exception) {
            e.printStackTrace()
            db.close()
            return false
        }
    }

    @SuppressLint("Range")
    fun getNameReaders(): List<String> {
        val readerList = mutableListOf<String>()
        val db = this.readableDatabase

        val query = "SELECT lastName, firstName, middleName, phone FROM Reader"
        val cursor: Cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val lastName = cursor.getString(cursor.getColumnIndex("lastName"))
            val firstName = cursor.getString(cursor.getColumnIndex("firstName"))
            val middleName = cursor.getString(cursor.getColumnIndex("middleName"))
            val phone = cursor.getString(cursor.getColumnIndex("phone"))

            val fullName = "$lastName $firstName $middleName $phone"
            readerList.add(fullName)
        }

        cursor.close()
        db.close()

        return readerList
    }

    @SuppressLint("Range")
    fun insertIssue(selectedBook: String, selectedReader: String, librarianId: Int): String {
        val db = this.writableDatabase

        val checkBookQuery = "SELECT id, availableCopies FROM Book WHERE title = ?"
        val checkBookCursor = db.rawQuery(checkBookQuery, arrayOf(selectedBook))

        if (checkBookCursor.moveToFirst()) {
            val bookId = checkBookCursor.getInt(checkBookCursor.getColumnIndex("id"))
            val availableCopies = checkBookCursor.getInt(checkBookCursor.getColumnIndex("availableCopies"))

            if (availableCopies > 0) {
                val updateBookQuery = "UPDATE Book SET availableCopies = ? WHERE id = ?"
                db.execSQL(updateBookQuery, arrayOf(availableCopies - 1, bookId))

                val readerNameParts = selectedReader.split(" ")
                val lastName = readerNameParts.getOrNull(0) ?: ""
                val firstName = readerNameParts.getOrNull(1) ?: ""
                val middleName = readerNameParts.getOrNull(2) ?: ""
                val phone = readerNameParts.getOrNull(3) ?: ""

                val getReaderIdQuery =
                    "SELECT id FROM Reader WHERE lastName = ? AND firstName = ? AND middleName = ? AND phone = ?"
                val readerIdCursor = db.rawQuery(getReaderIdQuery, arrayOf(lastName, firstName, middleName, phone))

                if (readerIdCursor.moveToFirst()) {
                    val readerId = readerIdCursor.getInt(readerIdCursor.getColumnIndex("id"))

                    val updateReaderQuery = "UPDATE Reader SET takenBooks = takenBooks + 1 WHERE id = ?"
                    db.execSQL(updateReaderQuery, arrayOf(readerId))

                    val insertIssueQuery =
                        "INSERT INTO Issue (issueDate, returnDate, status, book_id, reader_id, librarian_id) VALUES (?,?,?,?,?,?)"
                    val formattedDate = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(System.currentTimeMillis()))
                    val values = ContentValues()
                    values.put("issueDate", formattedDate)
                    values.put("returnDate", 0)
                    values.put("status", "Видано")
                    values.put("book_id", bookId)
                    values.put("reader_id", readerId)
                    values.put("librarian_id", librarianId)

                    val insertedId = db.insert("Issue", null, values).toInt()

                    checkBookCursor.close()
                    readerIdCursor.close()
                    db.close()
                    return "Запис успішний"
                } else {
                    checkBookCursor.close()
                    readerIdCursor.close()
                    db.close()
                    return "Помилка: Читача не існує в базі"
                }

            } else {
                checkBookCursor.close()
                db.close()
                return "Помилка: Даної книги немає в наявності"
            }
        } else {
            checkBookCursor.close()
            db.close()
            return "Помилка: Даної книги не існує"
        }
    }

    @SuppressLint("Range")
    fun getAllIssue(): ArrayList<String> {
        val issueList = ArrayList<String>()
        val db = this.readableDatabase

        val query = """
            SELECT Issue.id as id, issueDate, returnDate, status, Book.title as bookTitle, 
                   Reader.lastName as readerLastName, Reader.firstName as readerFirstName, 
                   Reader.middleName as readerMiddleName, Reader.phone as readerPhone 
            FROM Issue 
            INNER JOIN Book ON Issue.book_id = Book.id 
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            WHERE status = 'Видано' """
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val issueDate = cursor.getString(cursor.getColumnIndex("issueDate"))
            val returnDate = cursor.getString(cursor.getColumnIndex("returnDate"))
            val status = cursor.getString(cursor.getColumnIndex("status"))
            val bookTitle = cursor.getString(cursor.getColumnIndex("bookTitle"))
            val readerLastName = cursor.getString(cursor.getColumnIndex("readerLastName"))
            val readerFirstName = cursor.getString(cursor.getColumnIndex("readerFirstName"))
            val readerMiddleName = cursor.getString(cursor.getColumnIndex("readerMiddleName"))
            val readerPhone = cursor.getString(cursor.getColumnIndex("readerPhone"))

            val issueInfo = "ID: $id\n" +
                    "Issue Date: $issueDate\n" +
                    "Return Date: $returnDate\n" +
                    "Status: $status\n" +
                    "Book Title: $bookTitle\n" +
                    "Reader: $readerLastName $readerFirstName $readerMiddleName\n" +
                    "Phone: $readerPhone\n"

            issueList.add(issueInfo)
        }

        cursor.close()
        db.close()
        return issueList
    }

    @SuppressLint("Range")
    fun returnIssue(selectedBook: String, selectedReader: String): String {
        val db = this.writableDatabase

        // Отримати інформацію про видану книгу
        val getIssueInfoQuery = """
        SELECT Issue.id, issueDate, returnDate, status, Book.title as bookTitle, 
               Reader.lastName as readerLastName, Reader.firstName as readerFirstName, 
               Reader.middleName as readerMiddleName, Reader.phone as readerPhone 
        FROM Issue 
        INNER JOIN Book ON Issue.book_id = Book.id 
        INNER JOIN Reader ON Issue.reader_id = Reader.id
        WHERE status = 'Видано' AND Book.title = ? AND Reader.lastName || ' ' || Reader.firstName || ' ' || Reader.middleName || ' ' || Reader.phone = ?
    """
        val issueInfoCursor = db.rawQuery(getIssueInfoQuery, arrayOf(selectedBook, selectedReader))

        if (issueInfoCursor.moveToFirst()) {
            val issueId = issueInfoCursor.getInt(issueInfoCursor.getColumnIndex("id"))
            val bookTitle = issueInfoCursor.getString(issueInfoCursor.getColumnIndex("bookTitle"))
            val readerLastName = issueInfoCursor.getString(issueInfoCursor.getColumnIndex("readerLastName"))
            val readerFirstName = issueInfoCursor.getString(issueInfoCursor.getColumnIndex("readerFirstName"))
            val readerMiddleName = issueInfoCursor.getString(issueInfoCursor.getColumnIndex("readerMiddleName"))
            val readerPhone = issueInfoCursor.getString(issueInfoCursor.getColumnIndex("readerPhone"))

            val getBookIdQuery = "SELECT id FROM Book WHERE title = ?"
            val bookIdCursor = db.rawQuery(getBookIdQuery, arrayOf(bookTitle))

            val getReaderIdQuery =
                "SELECT id FROM Reader WHERE lastName = ? AND firstName = ? AND middleName = ? AND phone = ?"
            val readerIdCursor = db.rawQuery(getReaderIdQuery, arrayOf(readerLastName, readerFirstName, readerMiddleName, readerPhone))

            if (bookIdCursor.moveToFirst() && readerIdCursor.moveToFirst()) {
                val bookId = bookIdCursor.getInt(bookIdCursor.getColumnIndex("id"))
                val readerId = readerIdCursor.getInt(readerIdCursor.getColumnIndex("id"))

                val updateBookQuery = "UPDATE Book SET availableCopies = availableCopies + 1 WHERE id = ?"
                db.execSQL(updateBookQuery, arrayOf(bookId))

                val updateReaderQuery = "UPDATE Reader SET takenBooks = takenBooks - 1 WHERE id = ?"
                db.execSQL(updateReaderQuery, arrayOf(readerId))

                val updateIssueQuery = "UPDATE Issue SET returnDate = ?, status = 'Повернуто' WHERE id = ?"
                val formattedDate = SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(System.currentTimeMillis()))
                db.execSQL(updateIssueQuery, arrayOf(formattedDate, issueId))

                issueInfoCursor.close()
                bookIdCursor.close()
                readerIdCursor.close()
                db.close()
                return "Книга успішно повернена"
            } else {
                issueInfoCursor.close()
                bookIdCursor.close()
                readerIdCursor.close()
                db.close()
                return "Помилка: Невірна інформація про книгу або читача"
            }
        } else {
            issueInfoCursor.close()
            db.close()
            return "Помилка: Книга не була видана читачеві"
        }
    }

    @SuppressLint("Range")
    fun filterBooksReaders(tableName: String, columnName: String): ArrayList<String> {
        val paramsList = ArrayList<String>()
        val db = this.readableDatabase

        val query = "SELECT $columnName FROM $tableName"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val rez = cursor.getString(cursor.getColumnIndex(columnName))
            if (!paramsList.contains(rez)) {
                paramsList.add(rez)
            }
        }

        cursor.close()
        db.close()
        return paramsList
    }

    @SuppressLint("Range")
    fun filterIssue(columnName: String): ArrayList<String> {
        val paramsList = ArrayList<String>()
        val db = this.readableDatabase

        val query = """SELECT $columnName 
            FROM Issue 
            INNER JOIN Book ON Issue.book_id = Book.id 
            INNER JOIN Reader ON Issue.reader_id = Reader.id"""
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val rez = cursor.getString(cursor.getColumnIndex(columnName))
            if (!paramsList.contains(rez)) {
                paramsList.add(rez)
            }
        }

        cursor.close()
        db.close()
        return paramsList
    }

    @SuppressLint("Range")
    fun getBooksFromParam(param: String, value: String): ArrayList<String> {
        val booksList = ArrayList<String>()
        val db = this.readableDatabase
        val query = "SELECT * FROM Book WHERE $param = ?"
        val cursor = db.rawQuery(query, arrayOf(value))

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val title = cursor.getString(cursor.getColumnIndex("title"))
            val author = cursor.getString(cursor.getColumnIndex("author"))
            val publicationYear = cursor.getInt(cursor.getColumnIndex("publicationYear"))
            val isbn = cursor.getString(cursor.getColumnIndex("isbn"))
            val genre = cursor.getString(cursor.getColumnIndex("genre"))
            val pageCount = cursor.getInt(cursor.getColumnIndex("pageCount"))
            val availableCopies = cursor.getInt(cursor.getColumnIndex("availableCopies"))
            val publisher = cursor.getString(cursor.getColumnIndex("publisher"))

            val bookInfo = "ID: $id\n" +
                    "Title: $title\n" +
                    "Author: $author\n" +
                    "Publication Year: $publicationYear\n" +
                    "ISBN: $isbn\n" +
                    "Genre: $genre\n" +
                    "Page Count: $pageCount\n" +
                    "Available Copies: $availableCopies\n" +
                    "Publisher: $publisher\n"

            booksList.add(bookInfo)
        }

        cursor.close()
        db.close()
        return booksList
    }

    @SuppressLint("Range")
    fun getReadersFromParam(param: String, value: String): ArrayList<String> {
        val readersList = ArrayList<String>()
        val db = this.readableDatabase
        val query = "SELECT * FROM Reader WHERE $param = ?"
        val cursor = db.rawQuery(query, arrayOf(value))

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val lastName = cursor.getString(cursor.getColumnIndex("lastName"))
            val firstName = cursor.getString(cursor.getColumnIndex("firstName"))
            val middleName = cursor.getString(cursor.getColumnIndex("middleName"))
            val address = cursor.getString(cursor.getColumnIndex("address"))
            val phone = cursor.getString(cursor.getColumnIndex("phone"))
            val email = cursor.getString(cursor.getColumnIndex("email"))
            val takenBooks = cursor.getInt(cursor.getColumnIndex("takenBooks"))

            val readerInfo = "ID: $id\n" +
                    "Last Name: $lastName\n" +
                    "First Name: $firstName\n" +
                    "Middle Name: $middleName\n" +
                    "Address: $address\n" +
                    "Phone: $phone\n" +
                    "Email: $email\n" +
                    "Taken Books: $takenBooks\n"

            readersList.add(readerInfo)
        }

        cursor.close()
        db.close()
        return readersList
    }

    @SuppressLint("Range")
    fun getIssueFromParam(param: String, value: String): ArrayList<String> {
        val issueList = ArrayList<String>()
        val db = this.readableDatabase
        val query = """
            SELECT Issue.id as id, issueDate, returnDate, status, Book.title as bookTitle, 
                   Reader.lastName as readerLastName, Reader.firstName as readerFirstName, 
                   Reader.middleName as readerMiddleName, Reader.phone as readerPhone 
            FROM Issue 
            INNER JOIN Book ON Issue.book_id = Book.id 
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            WHERE $param = ?"""
        val cursor = db.rawQuery(query, arrayOf(value))

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("id"))
            val issueDate = cursor.getString(cursor.getColumnIndex("issueDate"))
            val returnDate = cursor.getString(cursor.getColumnIndex("returnDate"))
            val status = cursor.getString(cursor.getColumnIndex("status"))
            val bookTitle = cursor.getString(cursor.getColumnIndex("bookTitle"))
            val readerLastName = cursor.getString(cursor.getColumnIndex("readerLastName"))
            val readerFirstName = cursor.getString(cursor.getColumnIndex("readerFirstName"))
            val readerMiddleName = cursor.getString(cursor.getColumnIndex("readerMiddleName"))
            val readerPhone = cursor.getString(cursor.getColumnIndex("readerPhone"))

            val issueInfo = "ID: $id\n" +
                    "Issue Date: $issueDate\n" +
                    "Return Date: $returnDate\n" +
                    "Status: $status\n" +
                    "Book Title: $bookTitle\n" +
                    "Reader: $readerLastName $readerFirstName $readerMiddleName\n" +
                    "Phone: $readerPhone\n"

            issueList.add(issueInfo)
        }

        cursor.close()
        db.close()
        return issueList
    }
}
