from sanic import Sanic, response
import yaml
from database_manager import DatabaseManager
import asyncio
from datetime import datetime


with open('param.yml', 'r') as stream:
    try:
        db_dict = yaml.safe_load(stream)["database"]
        db_manager = DatabaseManager(db_dict["db_min_con"], db_dict["db_max_con"], db_dict["host"],
                                     db_dict["db_port"], db_dict["db_user"], db_dict["db_password"],
                                     db_dict["db_name"], db_dict["pool_name"])
    except yaml.YAMLError as exc:
        print(exc)

async def create_run():
    extension_query = "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"

    create_tables_query = """
    CREATE TABLE IF NOT EXISTS Library (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        name TEXT NOT NULL,
        systemPassword TEXT NOT NULL,
        address TEXT
    );

    CREATE TABLE IF NOT EXISTS Book (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        title TEXT NOT NULL,
        author TEXT NOT NULL,
        publicationYear INTEGER,
        isbn TEXT NOT NULL,
        genre TEXT,
        pageCount INTEGER,
        availableCopies INTEGER,
        publisher TEXT,
        library_id UUID REFERENCES Library(id) ON DELETE CASCADE ON UPDATE CASCADE
    );

    CREATE TABLE IF NOT EXISTS Reader (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        lastName TEXT NOT NULL,
        firstName TEXT NOT NULL,
        middleName TEXT,
        address TEXT,
        phone TEXT NOT NULL,
        email TEXT,
        client_id UUID REFERENCES Client(id) ON DELETE CASCADE ON UPDATE CASCADE,
        takenBooks INTEGER DEFAULT 0,
        library_id UUID REFERENCES Library(id) ON DELETE CASCADE ON UPDATE CASCADE
    );

    CREATE TABLE IF NOT EXISTS Librarian (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        lastName TEXT NOT NULL,
        firstName TEXT NOT NULL,
        middleName TEXT,
        address TEXT NOT NULL,
        phone TEXT NOT NULL,
        email TEXT,
        login TEXT NOT NULL,
        password TEXT NOT NULL,
        deleted BOOLEAN DEFAULT false,
        library_id UUID REFERENCES Library(id) ON DELETE CASCADE ON UPDATE CASCADE
    );

    CREATE TABLE IF NOT EXISTS Issue (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        issueDate DATE NOT NULL,
        returnDate DATE,
        status TEXT NOT NULL,
        book_id UUID REFERENCES Book(id) ON DELETE CASCADE ON UPDATE CASCADE,
        reader_id UUID REFERENCES Reader(id) ON DELETE CASCADE ON UPDATE CASCADE,
        librarian_id UUID REFERENCES Librarian(id) ON DELETE CASCADE ON UPDATE CASCADE
    );
    
    CREATE TABLE IF NOT EXISTS Client (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        lastName TEXT NOT NULL,
        firstName TEXT NOT NULL,
        middleName TEXT,
        address TEXT,
        phone TEXT NOT NULL,
        email TEXT,
        login TEXT NOT NULL,
        password TEXT NOT NULL
    );
    """

    await db_manager.modify_data_in_db(extension_query, None)

    await db_manager.modify_data_in_db(create_tables_query, None)

    # тест
    result = await db_manager.get_all_books({'client_id': '474f3e6e-87c8-4f23-9149-cebfd7436be5'})
    print(result)

app = Sanic("MySanicApp")

@app.route('/', methods=['POST'])
async def hello(request):
    print("hello")
    return response.json({"result": "OK"})

@app.route('/database', methods=['POST'])
async def db_function(request):
    try:
        data = request.json
        print(data)
        function_to_call = getattr(db_manager, data.get('function_name'), None)
        if function_to_call:
            if callable(function_to_call):
                result = await function_to_call(data.get('param_dict'))
                print(result)
                return response.json({"result": result})
            else:
                print(1)
                return response.json({"result": 0})
        else:
            print(2)
            return response.json({"result": 0})
    except Exception as e:
        print(f"An error occurred: {e}")
        return response.json({"error": "An internal error occurred"}, status=500)

if __name__ == '__main__':
    asyncio.run(create_run())
    app.run(host='0.0.0.0', port=5000)