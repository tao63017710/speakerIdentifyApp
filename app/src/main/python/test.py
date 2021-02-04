import numpy as np


def batch_cosine_similarity(x1, x2):
    mul = np.multiply(x1, x2)
    s = np.sum(mul, axis=1)

    return s