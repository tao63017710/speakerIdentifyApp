from random import choice

import numpy as np

from audio import pad_mfcc



def sample_from_mfcc(mfcc, max_length):
    if mfcc.shape[0] >= max_length:
        r = choice(range(0, len(mfcc) - max_length + 1))
        s = mfcc[r:r + max_length]
    else:
        s = pad_mfcc(mfcc, max_length)
    return np.expand_dims(s, axis=-1)
