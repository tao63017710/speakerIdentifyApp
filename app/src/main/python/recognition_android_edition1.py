import numpy as np
import random
import os
import time
import tensorflow as tf
from batcher import sample_from_mfcc
from audio import read_mfcc
from constants import SAMPLE_RATE, NUM_FRAMES
from test import batch_cosine_similarity
from os.path import dirname, join
#######################################################
# 从numpy文件读取语料集信息，不用跑模型 整合成函数 输出文件名


# 控制输出
def out_format(str_out, num):
    if len(str(num)) < 5:
        return str_out, str(num)[:len(str(num))]
    else:
        return str_out, str(num)[:5]


def distinguish(file_path, voice_root_path):
    try:
        np.random.seed(123)
        random.seed(123)

        interpreter = tf.lite.Interpreter(model_path=join(dirname(__file__), 'model.tflite'))
        interpreter.allocate_tensors()

        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()

        if os.path.exists(file_path):
            test_audio = sample_from_mfcc(read_mfcc(file_path, SAMPLE_RATE), NUM_FRAMES)
            test_audio = test_audio.astype(np.float32)
            interpreter.set_tensor(input_details[0]['index'], np.expand_dims(test_audio, axis=0))
            interpreter.invoke()
            test_predict = interpreter.get_tensor(output_details[0]['index'])

            all_audio = []
            for root, dirs, files in os.walk(join(voice_root_path, 'wave_numpy')):
                root = root.replace('\\', '/')
                for file in files:
                    if file.endswith('npy'):
                        all_audio.append((root + '/' + file, np.load(root + '/' + file)))

            if len(all_audio) > 0:
                print('use exist numpy')
                result = []
                for audio in all_audio:
                    result.append((audio[0], batch_cosine_similarity(test_predict, audio[1])))

            else:
                print('use original corpus')
                all_addr = []
                for root, dirs, files in os.walk(join(voice_root_path, 'wave_original')):
                    root = root.replace('\\', '/')
                    for file in files:
                        if file.endswith('flac') or file.endswith('wav'):
                            all_addr.append(root + '/' + file)

                audio_all = []
                for addr in all_addr:
                    audio = sample_from_mfcc(read_mfcc(addr, SAMPLE_RATE), NUM_FRAMES)
                    audio = audio.astype(np.float32)
                    interpreter.set_tensor(input_details[0]['index'], np.expand_dims(audio, axis=0))
                    interpreter.invoke()
                    predict_one = interpreter.get_tensor(output_details[0]['index'])
                    audio_all.append((addr, predict_one))

                result = []
                for audio in audio_all:
                    result.append((audio[0], batch_cosine_similarity(test_predict, audio[1])))


            cos_max = (result[0][1], result[0][0])
            for i, print_out in enumerate(result):
                if print_out[1] > cos_max[0][0]:
                    cos_max = (print_out[1], print_out[0])

            if cos_max[0] > 0.60:
                return out_format(cos_max[1], cos_max[0].item())
            else:
                return 'dont exist', 0
        else:
            return 'no wave input', 0
    except Exception:
        return 'error', 0


# test = distinguish(directory='../audio_test')
# print(test[0], test[1])

