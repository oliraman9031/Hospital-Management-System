CREATE TABLE IF NOT EXISTS adminreg (
    username VARCHAR(120) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS login (
    username VARCHAR(120) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS patient (
    fname VARCHAR(100),
    lname VARCHAR(100),
    gender VARCHAR(30),
    city VARCHAR(100),
    email VARCHAR(255),
    age VARCHAR(30),
    address TEXT,
    date VARCHAR(50),
    mobile VARCHAR(50) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS doctor (
    id INTEGER PRIMARY KEY,
    fname VARCHAR(100),
    lname VARCHAR(100),
    gender VARCHAR(30),
    mobile VARCHAR(100),
    city VARCHAR(100),
    email VARCHAR(255),
    age VARCHAR(30),
    address TEXT,
    date VARCHAR(50),
    qualification VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS recp (
    fname VARCHAR(100),
    lname VARCHAR(100),
    mobile VARCHAR(100) PRIMARY KEY,
    date VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS worker (
    fname VARCHAR(100),
    lname VARCHAR(100),
    mobile VARCHAR(100) PRIMARY KEY,
    date VARCHAR(50)
);

INSERT INTO adminreg (username, password)
VALUES ('admin', 'admin')
ON CONFLICT (username) DO NOTHING;
