FROM python:3.12-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY . .

# Create data directory with proper permissions
RUN mkdir -p /app/data && chmod 777 /app/data

EXPOSE 8103

# Run the application on port 8103
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8103"]
