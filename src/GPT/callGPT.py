import openai
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv("src\GPT\.env")

# Get the API key from the environment
api_key = os.getenv("OPENAI_API_KEY")

# Set your OpenAI API key
openai.api_key = api_key

# Prompt for text generation
prompt = "Once upon a time in a land far, far away"

# Generate text using the GPT-3 model
response = openai.Completion.create(
    engine="text-davinci-003",  # Use the GPT-3 engine
    prompt=prompt,
    max_tokens=50  # Set the maximum number of tokens in the generated output
)

# Print the generated text
print(response.choices[0].text.strip())