from typing import Union
from psycopg_pool import ConnectionPool
from psycopg import Connection
from datetime import datetime

class DatabaseManager:
    __slots__ = ('__connection_pool',)

    def __init__(self, min_conn_num: int, max_conn_num: int, host: str, port: int, user: str, password: str,
                 database: str, pool_name: str):
        self.__connection_pool = ConnectionPool(
            conninfo=f"host={host} port={port} dbname={database} user={user} password={password}",
            connection_class=Connection,
            min_size=min_conn_num,
            max_size=max_conn_num,
            open=True,
            name=pool_name,
            timeout=180.0,
        )

    @property
    def connection_pool(self):
        return self.__connection_pool

    def close_all_connections(self):
        self.__connection_pool.closeall()

    async def fetch_data_from_db(self, query: str, parameters: Union[list, dict] = None, fetchone: bool = False):
        with self.__connection_pool.connection() as free_connection:
            with free_connection as connection, connection.cursor() as cursor:
                cursor.execute(query, parameters)
                if fetchone:
                    data = cursor.fetchone()
                else:
                    data = cursor.fetchall()
                return data

    async def modify_data_in_db(self, query: str, parameters: dict):
        with self.__connection_pool.connection() as free_connection:
            with free_connection as connection, connection.cursor() as cursor:
                cursor.execute(query, parameters)
                return cursor.rowcount

    #Library
    async def add_library(self, param_dict: dict):
        library_name = param_dict.get("library_name")
        if await self.check_library_credentials({"library_name": library_name}):
            return -1
        query = """
        INSERT INTO public.library (name, systempassword, address)
        VALUES (%s, %s, %s);
        """
        parameters = (library_name, param_dict.get("systempassword"), param_dict.get("address"))
        library_new = await self.modify_data_in_db(query, parameters)

        return library_new if library_new else -1

    async def check_library_credentials(self, param_dict: dict):
        if param_dict.get("system_password"):
            query = "SELECT id::text FROM Library WHERE name = %s AND systempassword = %s"
            parameters = (param_dict.get("library_name"), param_dict.get("system_password"))
            data = await self.fetch_data_from_db(query, parameters, True)
            return data[0]
        else:
            query = "SELECT id::text FROM Library WHERE name = %s"
            parameters = (param_dict.get("library_name"),)
            data = await self.fetch_data_from_db(query, parameters, True)
            return bool(data)

    async def get_name_libraries(self, param_dict: dict):
        query = "SELECT name FROM Library"
        data = await self.fetch_data_from_db(query)
        libraries_list = [library[0] for library in data]
        return libraries_list

    #Librarian
    async def check_librarian_credentials(self, param_dict: dict):
        if param_dict.get("password"):
            query = "SELECT id::text, library_id::text FROM Librarian WHERE login = %s AND password = %s"
            parameters = (param_dict.get("login"), param_dict.get("password"))
            data = await self.fetch_data_from_db(query, parameters, True)
            return data if data else -1
        else:
            query = "SELECT id::text FROM Librarian WHERE login = %s"
            parameters = (param_dict.get("login"),)
            data = await self.fetch_data_from_db(query, parameters, True)
            return bool(data)


    async def add_librarian(self, param_dict: dict):
        if await self.check_librarian_credentials({"login": param_dict.get("login")}):
            return -1
        library = await self.check_library_credentials(param_dict)
        if not library:
            return -2
        query = """
        INSERT INTO Librarian (lastName, firstName, middleName, address, phone, email, login, password, library_id)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        RETURNING id::text, library_id::text
        """
        parameters = (param_dict.get("last_name"), param_dict.get("user_name"), param_dict.get("sur_name"), param_dict.get("address"), param_dict.get("phone"), param_dict.get("email"), param_dict.get("login"), param_dict.get("password"), library)
        data = await self.fetch_data_from_db(query, parameters, True)

        return data if data else -1

    #Book
    async def book_exists(self, param_dict: dict):
        if param_dict.get("library_id"):
            query = f"SELECT * FROM Book WHERE {param_dict.get("key")} = %s and library_id = %s"
            parameters = (param_dict.get("param"), param_dict.get("library_id"))
            data = await self.fetch_data_from_db(query, parameters, True)
        else:
            query = f"SELECT * FROM Book WHERE {param_dict.get("key")} = %s"
            parameters = (param_dict.get("param"),)
            data = await self.fetch_data_from_db(query, parameters, True)
        return bool(data)

    async def add_book(self, param_dict: dict):
        isbn = param_dict.get("isbn")
        library_id = param_dict.get("library_id")
        if await self.book_exists({"isbn": isbn, "key": "isbn", "library_id": library_id}):
            return -1
        query = """
        INSERT INTO Book (title, author, publicationYear, isbn, genre, pageCount, availableCopies, publisher, library_id)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        parameters = (param_dict.get("title"), param_dict.get("author"), param_dict.get("publication_year"), isbn, param_dict.get("genre"), param_dict.get("page_count"), param_dict.get("available_copies"), param_dict.get("publisher"), library_id)
        rows_new = await self.modify_data_in_db(query, parameters)
        return rows_new

    async def update_book(self, param_dict: dict):
        query = """
        UPDATE Book
        SET title = %s, author = %s, publicationYear = %s, isbn = %s, genre = %s,
        pageCount = %s, availableCopies = %s, publisher = %s
        WHERE id = %s
        """
        parameters = (
            param_dict.get('title'),
            param_dict.get('author'),
            param_dict.get('publication_year'),
            param_dict.get('isbn'),
            param_dict.get('genre'),
            param_dict.get('page_count'),
            param_dict.get('available_copies'),
            param_dict.get('publisher'),
            param_dict.get('book_id')
        )
        rows_affected = await self.modify_data_in_db(query, parameters)
        if rows_affected == 0:
            rows_affected = -1
        return rows_affected

    async def get_all_books(self, param_dict: dict):
        query = "SELECT * FROM Book WHERE library_id = %s"
        parameters = (param_dict.get('library_id'),)
        books_data = await self.fetch_data_from_db(query, parameters)
        return await self.get_book_view(books_data)

    async def delete_book(self, param_dict: dict):
        book_id = param_dict.get('book_id')
        if not await self.book_exists({"param": book_id, "key": "id"}):
            return False
        query = "DELETE FROM Book WHERE id = %s"
        parameters = (book_id,)
        rows_affected = await self.modify_data_in_db(query, parameters)
        if rows_affected == 0:
            rows_affected = -1
        return rows_affected

    async def get_name_books(self, param_dict: dict):
        query = "SELECT DISTINCT title FROM Book WHERE library_id = %s"
        parameters = (param_dict.get("library_id"),)
        data = await self.fetch_data_from_db(query, parameters)
        books_list = [book[0] for book in data]
        return books_list

    #Reader
    async def reader_exists(self, param_dict: dict):
        if param_dict.get("library_id"):
            query = f"SELECT * FROM Reader WHERE {param_dict.get('key')} = %s and library_id = %s"
            parameters = (param_dict.get('param'), param_dict.get('library_id'))
            data = await self.fetch_data_from_db(query, parameters, True)
        else:
            query = f"SELECT * FROM Reader WHERE {param_dict.get('key')} = %s"
            parameters = (param_dict.get('param'),)
            data = await self.fetch_data_from_db(query, parameters, True)
        return bool(data)

    async def add_reader(self, param_dict: dict):
        query = """
        INSERT INTO Reader (lastName, firstName, middleName, address, phone, email, takenBooks, library_id)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """
        parameters = (
            param_dict.get('last_name'),
            param_dict.get('first_name'),
            param_dict.get('middle_name'),
            param_dict.get('address'),
            param_dict.get('phone'),
            param_dict.get('email'),
            0,
            param_dict.get('library_id')
        )
        reader_new = await self.modify_data_in_db(query, parameters)
        if reader_new == 0:
            reader_new = -1
        return reader_new

    async def update_reader(self, param_dict: dict):
        query = """
        UPDATE Reader
        SET lastName = %s, firstName = %s, middleName = %s, address = %s, phone = %s, email = %s
        WHERE id = %s
        """
        parameters = (
            param_dict.get('last_name'),
            param_dict.get('first_name'),
            param_dict.get('middle_name'),
            param_dict.get('address'),
            param_dict.get('phone'),
            param_dict.get('email'),
            param_dict.get('reader_id')
        )
        rows_affected = await self.modify_data_in_db(query, parameters)
        if rows_affected == 0:
            rows_affected = -1
        return rows_affected

    async def get_all_readers(self, param_dict: dict):
        query = "SELECT * FROM Reader WHERE library_id = %s"
        parameters = (param_dict.get('library_id'),)
        readers_data = await self.fetch_data_from_db(query, parameters)
        return await self.get_reader_view(readers_data)

    async def delete_reader(self, param_dict: dict):
        reader_id = param_dict.get('reader_id')
        if not await self.reader_exists({'param': reader_id, 'key': 'id'}):
            return False
        query = "DELETE FROM Reader WHERE id = %s"
        parameters = (reader_id,)
        rows_affected = await self.modify_data_in_db(query, parameters)
        if rows_affected == 0:
            rows_affected = -1
        return rows_affected

    async def get_name_readers(self, param_dict: dict):
        query = "SELECT lastName, firstName, middleName, phone FROM Reader WHERE library_id = %s"
        parameters = (param_dict.get("library_id"),)
        data = await self.fetch_data_from_db(query, parameters)
        readers_list = [f"{reader[0]} {reader[1]} {reader[2]} {reader[3]}" for reader in data]
        return readers_list

    #Issue
    async def insert_issue(self, param_dict: dict):
        library_id = param_dict.get('library_id')

        check_book_query = "SELECT id::text, availableCopies FROM Book WHERE title = %s and library_id = %s"
        check_book_data = await self.fetch_data_from_db(check_book_query, [param_dict.get('selected_book'), library_id], True)

        if check_book_data:
            book_id, available_copies = check_book_data

            if available_copies > 0:
                reader_name_parts = param_dict.get('selected_reader').split()
                last_name, first_name, middle_name, phone = reader_name_parts + [""] * (4 - len(reader_name_parts))

                get_reader_id_query = "SELECT id::text FROM Reader WHERE lastName = %s AND firstName = %s AND middleName = %s AND phone = %s AND library_id = %s"
                reader_id = await self.fetch_data_from_db(get_reader_id_query,
                                                          [last_name, first_name, middle_name, phone, library_id], True)

                if reader_id:
                    reader_id = reader_id[0]

                    insert_issue_query = "INSERT INTO Issue (issueDate, status, book_id, reader_id, librarian_id) VALUES (%s, %s, %s, %s, %s)"
                    formatted_date = datetime.now().strftime("%d.%m.%Y %H:%M:%S")
                    values = (formatted_date, "Видано", book_id, reader_id, param_dict.get('librarian_id'))
                    await self.modify_data_in_db(insert_issue_query, values)

                    update_book_query = "UPDATE Book SET availableCopies = %s WHERE id = %s"
                    await self.modify_data_in_db(update_book_query, [available_copies - 1, book_id])

                    update_reader_query = "UPDATE Reader SET takenBooks = takenBooks + 1 WHERE id = %s"
                    await self.modify_data_in_db(update_reader_query, [reader_id, ])

                    return "Запис успішний"
                else:
                    return "Помилка: Читача не існує в базі"
            else:
                return "Помилка: Даної книги немає в наявності"
        else:
            return "Помилка: Даної книги не існує"

    async def get_all_issues(self, param_dict: dict):
        query = """
            SELECT Issue.id as id, issueDate, returnDate, status, Book.title as bookTitle, 
                   Reader.lastName as readerLastName, Reader.firstName as readerFirstName, 
                   Reader.middleName as readerMiddleName, Reader.phone as readerPhone 
            FROM Issue 
            INNER JOIN Book ON Issue.book_id = Book.id 
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            INNER JOIN Librarian ON Issue.librarian_id = Librarian.id
            WHERE status = 'Видано' AND Librarian.library_id = %s
        """
        parameters = (param_dict.get("library_id"),)
        issues_data = await self.fetch_data_from_db(query, parameters)
        return await self.get_issue_view(issues_data)

    async def return_issue(self, param_dict: dict):
        get_issue_info_query = """
        SELECT Issue.id::text, Book.id::text, Reader.id::text
        FROM Issue 
        INNER JOIN Book ON Issue.book_id = Book.id 
        INNER JOIN Reader ON Issue.reader_id = Reader.id
        INNER JOIN Librarian ON Issue.librarian_id = Librarian.id
        WHERE status = 'Видано' AND Book.title = %s 
        AND Reader.lastName || ' ' || Reader.firstName || ' ' || Reader.middleName || ' ' || Reader.phone = %s
        AND Librarian.library_id = %s
        """
        issue_info = await self.fetch_data_from_db(get_issue_info_query,
                                                   [param_dict.get('selected_book'), param_dict.get('selected_reader'),
                                                    param_dict.get('library_id')], True)

        if issue_info:
            issue_id = issue_info[0]
            book_id = issue_info[1]
            reader_id = issue_info[2]

            update_book_query = "UPDATE Book SET availableCopies = availableCopies + 1 WHERE id = %s"
            await self.modify_data_in_db(update_book_query, [book_id,])

            update_reader_query = "UPDATE Reader SET takenBooks = takenBooks - 1 WHERE id = %s"
            await self.modify_data_in_db(update_reader_query, [reader_id,])

            update_issue_query = "UPDATE Issue SET returnDate = %s, status = 'Повернуто' WHERE id = %s"
            formatted_date = datetime.now().strftime("%d.%m.%Y %H:%M:%S")
            await self.modify_data_in_db(update_issue_query, [formatted_date, issue_id])

            return "Книга успішно повернена"
        else:
            return "Помилка: Книга не була видана читачеві"

    async def filter_books_readers(self, param_dict: dict):
        query = f"SELECT DISTINCT {param_dict.get('column_name')} FROM {param_dict.get('table_name')} WHERE library_id = %s"
        params_list = await self.fetch_data_from_db(query, [param_dict.get('library_id'),])
        return [param[0] for param in params_list]

    async def delete_books_readers_for_param(self, param_dict: dict):
        query = f"DELETE FROM {param_dict.get('table_name')} WHERE {param_dict.get('column_name')} = %s AND library_id = %s"
        rows_affected = await self.modify_data_in_db(query, [param_dict.get('value'), param_dict.get('library_id')])
        if rows_affected == 0:
            rows_affected = -1
        return rows_affected

    async def filter_issue(self, param_dict: dict):
        query = f"""
            SELECT DISTINCT {param_dict.get('column_name')}
            FROM Issue
            INNER JOIN Book ON Issue.book_id = Book.id
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            WHERE Reader.library_id = %s
        """
        params_list = await self.fetch_data_from_db(query, [param_dict.get('library_id'),])
        return [param[0] for param in params_list]

    async def get_books_from_param(self, param_dict: dict):
        query = f"SELECT * FROM Book WHERE {param_dict.get('param')} = %s and library_id = %s"
        books_data = await self.fetch_data_from_db(query, [param_dict.get('value'), param_dict.get('library_id')])
        return await self.get_book_view(books_data)

    async def get_readers_from_param(self, param_dict: dict):
        query = f"SELECT * FROM Reader WHERE {param_dict.get('param')} = %s and library_id = %s"
        readers_data = await self.fetch_data_from_db(query, [param_dict.get('value'), param_dict.get('library_id')])
        return await self.get_reader_view(readers_data)

    async def get_issue_from_param(self, param_dict: dict):
        query = f"""
            SELECT Issue.id as id, issueDate, returnDate, status, Book.title as bookTitle, 
                   Reader.lastName as readerLastName, Reader.firstName as readerFirstName, 
                   Reader.middleName as readerMiddleName, Reader.phone as readerPhone 
            FROM Issue 
            INNER JOIN Book ON Issue.book_id = Book.id 
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            WHERE {param_dict.get('param')} = %s AND Reader.library_id = %s
        """
        issues_data = await self.fetch_data_from_db(query, [param_dict.get('value'), param_dict.get('library_id')])
        return await self.get_issue_view(issues_data)

    async def get_report_data(self, param_dict: dict):
        query = """
            SELECT
                CONCAT(Reader.lastName, ' ', Reader.firstName, ' ', Reader.middleName) AS readerFullName,
                Book.title,
                issueDate::text AS effectiveDate,
                'Видано' AS status  
            FROM
                Issue
                INNER JOIN Book ON Issue.book_id = Book.id 
                INNER JOIN Reader ON Issue.reader_id = Reader.id
                INNER JOIN Librarian ON Issue.librarian_id = Librarian.id
            WHERE
                issueDate BETWEEN %(date_from)s AND %(date_to)s AND
                Librarian.library_id = %(library_id)s
            UNION ALL
            SELECT
                CONCAT(Reader.lastName, ' ', Reader.firstName, ' ', Reader.middleName) AS readerFullName,
                Book.title,
                returnDate::text AS effectiveDate,
                'Повернуто' AS status
            FROM
                Issue
                INNER JOIN Book ON Issue.book_id = Book.id 
                INNER JOIN Reader ON Issue.reader_id = Reader.id
                INNER JOIN Librarian ON Issue.librarian_id = Librarian.id
            WHERE
                returnDate BETWEEN %(date_from)s AND %(date_to)s AND
                Librarian.library_id = %(library_id)s
            ORDER BY
                effectiveDate;
        """
        issues_data = await self.fetch_data_from_db(query, param_dict)
        return issues_data

    async def get_books_and_readers(self, param_dict: dict):
        books_name = await self.get_name_books(param_dict)
        readers_name = await self.get_name_readers(param_dict)
        return [books_name, readers_name]

    async def get_book_view(self, books_data: list):
        books_list = []
        for book in books_data:
            book_info = (
                f"ID: {book[0]}\n"
                f"Title: {book[1]}\n"
                f"Author: {book[2]}\n"
                f"Publication Year: {book[3]}\n"
                f"ISBN: {book[4]}\n"
                f"Genre: {book[5]}\n"
                f"Page Count: {book[6]}\n"
                f"Available Copies: {book[7]}\n"
                f"Publisher: {book[8]}\n"
            )
            books_list.append(book_info)
        return books_list

    async def get_reader_view(self, readers_data: list):
        readers_list = []
        for reader in readers_data:
            reader_info = (
                f"ID: {reader[0]}\n"
                f"Last Name: {reader[1]}\n"
                f"First Name: {reader[2]}\n"
                f"Middle Name: {reader[3]}\n"
                f"Address: {reader[4]}\n"
                f"Phone: {reader[5]}\n"
                f"Email: {reader[6]}\n"
                f"Taken Books: {reader[7]}\n"
            )
            readers_list.append(reader_info)
        return readers_list

    async def get_issue_view(self, issues_data: list):
        issues_list = []
        for issue in issues_data:
            issue_info = (
                f"ID: {issue[0]}\n"
                f"Issue Date: {issue[1]}\n"
                f"Return Date: {issue[2]}\n"
                f"Status: {issue[3]}\n"
                f"Book Title: {issue[4]}\n"
                f"Reader: {issue[5]} {issue[6]} {issue[7]}\n"
                f"Phone: {issue[8]}\n"
            )
            issues_list.append(issue_info)
        return issues_list
