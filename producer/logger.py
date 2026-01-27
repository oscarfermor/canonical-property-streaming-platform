# simple_logger.py
import logging
import sys


class SimpleLogger:
    """Simple logger class for basic logging needs."""

    def __init__(self, name: str = "app", level: str = "INFO"):
        self.logger = logging.getLogger(name)
        self.logger.setLevel(level.upper())

        # Only add handlers if none exist
        if not self.logger.handlers:
            # Console handler
            handler = logging.StreamHandler(sys.stdout)
            formatter = logging.Formatter(
                "%(asctime)s [%(levelname)s] %(message)s", datefmt="%H:%M:%S"
            )
            handler.setFormatter(formatter)
            self.logger.addHandler(handler)

    def info(self, message: str):
        self.logger.info(message)

    def debug(self, message: str):
        self.logger.debug(message)

    def warning(self, message: str):
        self.logger.warning(message)

    def error(self, message: str):
        self.logger.error(message)


# Create instance
logger = SimpleLogger()
