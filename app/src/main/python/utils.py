import logging
import os
import shutil
from glob import glob


logger = logging.getLogger(__name__)


def find_files(directory, ext='wav'):
    return sorted(glob(directory + f'/**/*.{ext}', recursive=True))


def create_new_empty_dir(directory: str):
    if os.path.exists(directory):
        shutil.rmtree(directory)
    os.makedirs(directory)


def ensure_dir_for_filename(filename: str):
    ensures_dir(os.path.dirname(filename))


def ensures_dir(directory: str):
    if len(directory) > 0 and not os.path.exists(directory):
        os.makedirs(directory)
