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

    #
    #   Library
    #

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

    async def get_all_libraries(self, param_dict: dict):
        query = "SELECT * FROM Library"
        libraries_data = await self.fetch_data_from_db(query)
        return await self.get_library_view(libraries_data)

    #
    #   Librarian
    #

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

    #
    #   Book
    #

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
        library_id = param_dict.get("library_id")

        if library_id:
            query = "SELECT * FROM Book WHERE library_id = %s"
            parameters = (library_id,)
        else:
            query = "SELECT * FROM Book"
            parameters = ()

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

    #
    #   Reader
    #

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

    #
    #   Issue
    #

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

    #
    # my_book
    #

    async def get_all_mybooks(self, param_dict: dict):
        query = """
            SELECT DISTINCT Book.*
            FROM Book
            INNER JOIN Issue ON Book.id = Issue.book_id
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            WHERE Reader.client_id = %s
        """
        books_data = await self.fetch_data_from_db(query, [param_dict.get('client_id'),])
        return await self.get_book_view(books_data)

    async def filter_mybooks(self, param_dict: dict):
        query = f"""
            SELECT DISTINCT Book.{param_dict.get('column_name')}
            FROM Book
            INNER JOIN Issue ON Book.id = Issue.book_id
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            WHERE Reader.client_id = %s
        """
        result = await self.fetch_data_from_db(query, [param_dict.get('client_id')])
        return [row[0] for row in result if row[0] is not None]

    async def get_mybooks_from_param(self, param_dict: dict):
        query = f"""
            SELECT DISTINCT Book.*
            FROM Book
            INNER JOIN Issue ON Book.id = Issue.book_id
            INNER JOIN Reader ON Issue.reader_id = Reader.id
            WHERE Reader.client_id = %s AND Book.{param_dict.get('param')} = %s
        """
        books_data = await self.fetch_data_from_db(query, [param_dict.get('client_id'), param_dict.get('value')])
        return await self.get_book_view(books_data)

    async def get_libraries_with_mybook(self, param_dict: dict):
        """
        Повертає бібліотеки, з яких клієнт брав книгу з певною назвою.
        """
        query = """
            SELECT DISTINCT L.name, L.address
            FROM Library L
            INNER JOIN Book B ON B.library_id = L.id
            INNER JOIN Issue I ON I.book_id = B.id
            INNER JOIN Reader R ON I.reader_id = R.id
            WHERE R.client_id = %s AND B.title = %s
        """
        libraries_data = await self.fetch_data_from_db(query, [
            param_dict.get("client_id"),
            param_dict.get("book_title")
        ])
        return [[lib[0], lib[1]] for lib in libraries_data] if libraries_data else []

    #
    # filter
    #

    async def filter_books_readers(self, param_dict: dict):
        query = f"SELECT DISTINCT {param_dict.get('column_name')} FROM {param_dict.get('table_name')} WHERE library_id = %s"
        params_list = await self.fetch_data_from_db(query, [param_dict.get('library_id'),])
        return [param[0] for param in params_list]

    async def filter_books(self, param_dict: dict):
        column_name = param_dict.get('column_name')
        if not column_name:
            return []
        query = f"""
            SELECT DISTINCT {column_name} 
            FROM Book
        """
        params_list = await self.fetch_data_from_db(query)
        return [param[0] for param in params_list] if params_list else []

    async def filter_libraries(self, param_dict: dict):
        column_name = param_dict.get('column_name')
        if not column_name:
            return []
        query = f"""
            SELECT DISTINCT {column_name} 
            FROM Library
        """
        params_list = await self.fetch_data_from_db(query)
        return [param[0] for param in params_list] if params_list else []

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

    #
    # delete
    #

    async def delete_books_readers_for_param(self, param_dict: dict):
        query = f"DELETE FROM {param_dict.get('table_name')} WHERE {param_dict.get('column_name')} = %s AND library_id = %s"
        rows_affected = await self.modify_data_in_db(query, [param_dict.get('value'), param_dict.get('library_id')])
        if rows_affected == 0:
            rows_affected = -1
        return rows_affected

    #
    # get_with_in
    #

    async def get_libraries_with_book(self, param_dict: dict):
        book_title = param_dict.get("book_title")
        if not book_title:
            return []

        query = """
            SELECT DISTINCT L.name, L.address
            FROM Library L
            JOIN Book B ON B.library_id = L.id
            WHERE B.title = %s
        """
        libraries_data = await self.fetch_data_from_db(query, [book_title,])
        return [[lib[0], lib[1]] for lib in libraries_data] if libraries_data else []

    async def get_books_in_library(self, param_dict: dict):
        library_id = param_dict.get("library_id")
        if not library_id:
            return []
        query = """
            SELECT title, author
            FROM Book
            WHERE library_id = %s
        """
        books_data = await self.fetch_data_from_db(query, [library_id,])
        return [[book[0], book[1]] for book in books_data] if books_data else []

    #
    #   get__from_param
    #

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

    async def get_libraries_from_param(self, param_dict: dict):
        query = f"SELECT * FROM Library WHERE {param_dict.get('param')} = %s"
        libraries_data = await self.fetch_data_from_db(query, [param_dict.get('value')])
        return await self.get_library_view(libraries_data)

    async def get_books_from_param(self, param_dict: dict):
        query = f"SELECT * FROM Book WHERE {param_dict.get('param')} = %s"
        books_data = await self.fetch_data_from_db(query, [param_dict.get('value')])
        return await self.get_book_view(books_data)

    #
    #   report
    #

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

    #
    #   get__view
    #

    async def get_books_and_readers(self, param_dict: dict):
        books_name = await self.get_name_books(param_dict)
        readers_name = await self.get_name_readers(param_dict)
        return [books_name, readers_name]

    async def get_library_view(self, libraries_data: list):
        libraries_list = []
        for library in libraries_data:
            library_info = (
                f"ID: {library[0]}\n"
                f"Name: {library[1]}\n"
                f"Address: {library[3]}\n"
            )
            libraries_list.append(library_info)
        return libraries_list

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
                f"Taken Books: {reader[8]}\n"
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

    #
    #   Функції для клієнта
    #

    async def check_client_credentials(self, param_dict: dict):
        if param_dict.get("password"):
            query = """
            SELECT id::text FROM Client
            WHERE login = %s AND password = %s
            """
            parameters = (param_dict.get("login"), param_dict.get("password"))
            data = await self.fetch_data_from_db(query, parameters, True)
            return data if data else -1
        else:
            query = "SELECT id::text FROM Client WHERE login = %s"
            parameters = (param_dict.get("login"),)
            data = await self.fetch_data_from_db(query, parameters, True)
            return bool(data)

    async def add_client(self, param_dict: dict):
        # Перевірка на дублювання логіну
        if await self.check_client_credentials({"login": param_dict.get("login")}):
            return -1
        # Запит на вставку нового клієнта
        query = """
        INSERT INTO Client (lastName, firstName, middleName, address, phone, email, login, password)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        RETURNING id::text
        """
        parameters = (
            param_dict.get("last_name"),
            param_dict.get("user_name"),
            param_dict.get("sur_name"),
            param_dict.get("address"),
            param_dict.get("phone"),
            param_dict.get("email"),
            param_dict.get("login"),
            param_dict.get("password"),
        )

        data = await self.fetch_data_from_db(query, parameters, True)
        return data if data else -1

    #
    #   Термінал
    #

    async def terminal_get_books(self, param_dict: dict):
        library_id = param_dict.get("library_id")
        if not library_id:
            return []
        query = """
            SELECT title, author, isbn
            FROM Book
            WHERE library_id = %s
        """
        books_data = await self.fetch_data_from_db(query, [library_id,])
        return [[book[0], book[1], book[2]] for book in books_data] if books_data else []

    async def terminal_get_mybooks(self, param_dict: dict):
        library_id = param_dict.get("library_id")
        client_id = param_dict.get("client_id")

        if not library_id or not client_id:
            return []

        query = """
            SELECT 
                b.title,
                b.author,
                b.isbn
            FROM Issue i
            JOIN Book b ON i.book_id = b.id
            JOIN Reader r ON i.reader_id = r.id
            WHERE 
                r.client_id = %s
                AND b.library_id = %s
                AND i.status = 'Видано'
        """

        books_data = await self.fetch_data_from_db(query, [client_id, library_id])
        return [[book[0], book[1], book[2]] for book in books_data] if books_data else []

    async def terminal_purchase_books(self, param_dict: dict):
        """
        Купівля книг (без client_id).
        Перевіряє, чи існують усі книги у потрібній кількості перед оновленням.
        """
        library_id = param_dict.get("library_id")
        list_isbn = param_dict.get("list_isbn", [])
        list_quantity = param_dict.get("list_quantity", [])

        if not library_id or not list_isbn:
            return "Помилка: некоректні параметри запиту"

        book_ids = []
        for isbn, qty in zip(list_isbn, list_quantity):
            check_query = "SELECT id::text, availableCopies FROM Book WHERE isbn = %s AND library_id = %s"
            book_data = await self.fetch_data_from_db(check_query, [isbn, library_id], True)

            if not book_data:
                return f"Помилка: книги з ISBN {isbn} не знайдено"
            book_id, available = book_data
            if available < qty:
                return f"Помилка: недостатньо примірників для ISBN {isbn}"

            book_ids.append((book_id, qty))

        for book_id, qty in book_ids:
            update_query = "UPDATE Book SET availableCopies = availableCopies - %s WHERE id = %s"
            await self.modify_data_in_db(update_query, [qty, book_id])

        return "Операція успішна"

    async def terminal_issue_books(self, param_dict: dict):
        """
        Видача книг клієнту (client_id обов’язковий).
        Якщо читача немає — створює нового.
        """
        library_id = param_dict.get("library_id")
        client_id = param_dict.get("client_id")
        librarian_id = param_dict.get("librarian_id")
        list_isbn = param_dict.get("list_isbn", [])
        list_quantity = param_dict.get("list_quantity", [])

        if not (library_id and client_id):
            return "Помилка: відсутні параметри бібліотеки або клієнта"

        # Перевірка існування читача
        get_reader_query = "SELECT id::text FROM Reader WHERE client_id = %s AND library_id = %s"
        reader_data = await self.fetch_data_from_db(get_reader_query, [client_id, library_id], True)

        if not reader_data:
            # Створення нового читача
            client_query = "SELECT lastName, firstName, middleName, address, phone, email FROM Client WHERE id = %s"
            client_info = await self.fetch_data_from_db(client_query, [client_id], True)
            if not client_info:
                return "Помилка: клієнта не знайдено"

            last_name, first_name, middle_name, address, phone, email = client_info
            insert_reader_query = """
                INSERT INTO Reader (lastName, firstName, middleName, address, phone, email, client_id, library_id, takenBooks)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 0)
                RETURNING id::text
            """
            reader_data = await self.fetch_data_from_db(
                insert_reader_query, [last_name, first_name, middle_name, address, phone, email, client_id, library_id],
                True
            )

        reader_id = reader_data[0]

        # --- Перевірка книг ---
        book_entries = []
        for isbn, qty in zip(list_isbn, list_quantity):
            check_query = "SELECT id::text, availableCopies FROM Book WHERE isbn = %s AND library_id = %s"
            book_data = await self.fetch_data_from_db(check_query, [isbn, library_id], True)

            if not book_data:
                return f"Помилка: книги з ISBN {isbn} не знайдено"
            book_id, available = book_data
            if available < qty:
                return f"Помилка: недостатньо примірників для ISBN {isbn}"

            book_entries.append((book_id, qty))

        # --- Виконуємо видачу ---
        for book_id, qty in book_entries:

            issue_query = """
                INSERT INTO Issue (issueDate, status, book_id, reader_id, librarian_id)
                SELECT %s, 'Видано', %s, %s, %s
                FROM generate_series(1, %s);
            """
            formatted_date = datetime.now().strftime("%d.%m.%Y %H:%M:%S")
            await self.modify_data_in_db(issue_query, [formatted_date, book_id, reader_id, librarian_id, qty])

            update_book_query = "UPDATE Book SET availableCopies = availableCopies - %s WHERE id = %s"
            await self.modify_data_in_db(update_book_query, [qty, book_id])

            update_reader_query = "UPDATE Reader SET takenBooks = takenBooks + %s WHERE id = %s"
            await self.modify_data_in_db(update_reader_query, [qty, reader_id])

        return "Операція успішна"

    async def terminal_return_books(self, param_dict: dict):
        """
        Повернення книг клієнтом.
        Перевіряє, що читач справді має ці книги на руках і не повертає більше, ніж отримав.
        """
        library_id = param_dict.get("library_id")
        client_id = param_dict.get("client_id")
        list_isbn = param_dict.get("list_isbn", [])
        list_quantity = param_dict.get("list_quantity", [])

        if not (library_id and client_id):
            return "Помилка: відсутні параметри бібліотеки або клієнта"

        # Знаходимо читача
        get_reader_query = "SELECT id::text FROM Reader WHERE client_id = %s AND library_id = %s"
        reader_data = await self.fetch_data_from_db(get_reader_query, [client_id, library_id], True)
        if not reader_data:
            return "Помилка: читача не знайдено"

        reader_id = reader_data[0]

        book_entries = []

        # --- Перевірка наявності виданих книг ---
        for isbn, qty in zip(list_isbn, list_quantity):
            # Перевіряємо, чи є в цього читача видані книги з цим ISBN
            check_query = """
                SELECT b.id::text, COUNT(i.id) AS issued_count
                FROM Book b
                JOIN Issue i ON i.book_id = b.id
                WHERE b.isbn = %s
                  AND b.library_id = %s
                  AND i.reader_id = %s
                  AND i.status = 'Видано'
                GROUP BY b.id;
            """
            result = await self.fetch_data_from_db(check_query, [isbn, library_id, reader_id], True)

            if not result:
                return f"Помилка: книга з ISBN {isbn} не була видана цьому читачу"

            book_id, issued_count = result
            if issued_count < qty:
                return f"Помилка: кількість для повернення ({qty}) перевищує кількість виданих ({issued_count}) для ISBN {isbn}"

            book_entries.append((book_id, qty))

        # --- Виконуємо оновлення ---
        for book_id, qty in book_entries:

            # Оновлюємо записи видач
            formatted_date = datetime.now().strftime("%d.%m.%Y %H:%M:%S")
            update_issue_query = """
                UPDATE Issue
                SET returnDate = %s, status = 'Повернено'
                WHERE id IN (
                    SELECT id FROM Issue
                    WHERE book_id = %s AND reader_id = %s AND status = 'Видано'
                    ORDER BY issueDate ASC
                    LIMIT %s
                )
            """
            await self.modify_data_in_db(update_issue_query, [formatted_date, book_id, reader_id, qty])

            # Повертаємо примірники
            update_book_query = "UPDATE Book SET availableCopies = availableCopies + %s WHERE id = %s"
            await self.modify_data_in_db(update_book_query, [qty, book_id])

            # Зменшуємо кількість у читача
            update_reader_query = "UPDATE Reader SET takenBooks = GREATEST(takenBooks - %s, 0) WHERE id = %s"
            await self.modify_data_in_db(update_reader_query, [qty, reader_id])

        return "Операція успішна"

    #
    # Sync
    #

    async def sync_reader_client(self, param_dict: dict):
        library_id = param_dict.get("library_id")
        login = param_dict.get("login")
        password = param_dict.get("password")

        if not (library_id and login and password):
            return "Помилка: недостатньо даних"

        query = """
            WITH client_data AS (
                SELECT id, lastName, firstName, middleName, phone
                FROM Client
                WHERE login = %s AND password = %s
            ),
            reader_match AS (
                SELECT r.id, r.client_id
                FROM Reader r
                JOIN client_data c
                  ON r.library_id = %s
                 AND r.lastName = c.lastName
                 AND r.firstName = c.firstName
                 AND (r.middleName = c.middleName OR (r.middleName IS NULL AND c.middleName IS NULL))
                 AND r.phone = c.phone
            ),
            updated AS (
                UPDATE Reader r
                SET client_id = (SELECT id FROM client_data)
                WHERE r.id IN (SELECT id FROM reader_match WHERE client_id IS NULL)
                RETURNING r.id
            )
            SELECT
                CASE
                    WHEN (SELECT id FROM client_data) IS NULL THEN -1
                    WHEN (SELECT id FROM reader_match) IS NULL THEN -2
                    WHEN (SELECT id FROM reader_match WHERE client_id IS NOT NULL LIMIT 1) IS NOT NULL THEN 0
                    WHEN (SELECT id FROM updated LIMIT 1) IS NOT NULL THEN 1
                    ELSE -99
                END AS result;
        """

        result_data = await self.fetch_data_from_db(query, [login, password, library_id], True)

        if not result_data:
            return "Сталася внутрішня помилка. Спробуйте ще раз або зверніться до службі підтримки"

        code = result_data[0]

        match code:
            case 1:
                return "Синхронізація успішна"
            case 0:
                return "Користувач вже синхронізований"
            case -1:
                return "Невірний логін чи пароль"
            case -2:
                return "Не вдалося знайти читача за відповідними даними користувача"
            case _:
                return "Сталася внутрішня помилка. Спробуйте ще раз або зверніться до службі підтримки"


