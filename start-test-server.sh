#!/bin/bash

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is not installed or not in your PATH."
    echo "Please install Python 3 to run this test server."
    exit 1
fi

mkdir -p test-server
cd test-server

# Create hello.txt if it doesn't exist
if [ ! -f "hello.txt" ]; then
  echo "Creating hello.txt..."
  echo "Gee, that's a lotta load!" > hello.txt
else
  echo "hello.txt already exists, skipping."
fi

# Create index.html if it doesn't exist
if [ ! -f "index.html" ]; then
  echo "Creating index.html..."
  echo "<html><body><h1>Hello, load tester &#128075;</h1></body></html>" > index.html
else
  echo "index.html already exists, skipping."
fi

echo "Test server files ready in ./test-server"
echo "Starting server on port 8000..."
python3 -m http.server 8000
