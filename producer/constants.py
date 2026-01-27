import os
from dotenv import load_dotenv


load_dotenv()


KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "")
SCHEMA_REGISTRY_URL = os.getenv("SCHEMA_REGISTRY_URL", "")
TOPIC = os.getenv("TOPIC", "")
SCHEMA_PATH = os.getenv("SCHEMA_PATH", "")
