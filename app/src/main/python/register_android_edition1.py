############################################################################
# testData/test_clean 包装成函数，接口调入文件夹
import numpy as np
import random
import os
import time
import tensorflow as tf
from batcher import sample_from_mfcc
from audio import read_mfcc
from constants import SAMPLE_RATE, NUM_FRAMES
from os.path import dirname, join

#######################################################


def save_wave_method(directory, destination):
    os.rename(directory, destination)


# 主方法
def register(file_path, numpy_addr):
    np.random.seed(123)
    random.seed(123)

    interpreter = tf.lite.Interpreter(model_path=join(dirname(__file__), 'model.tflite'))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    if os.path.exists(file_path):
        try:
            insert_audio = sample_from_mfcc(read_mfcc(file_path, SAMPLE_RATE), NUM_FRAMES)
            insert_audio = insert_audio.astype(np.float32)
            interpreter.set_tensor(input_details[0]['index'], np.expand_dims(insert_audio, axis=0))
            interpreter.invoke()
            insert_predict = interpreter.get_tensor(output_details[0]['index'])

            np.save(numpy_addr, insert_predict)
        except Exception:
            return False
        else:
            return True
    else:
        return False


# test = register(directory='../audio_test', name='sjy')
# print(test)
