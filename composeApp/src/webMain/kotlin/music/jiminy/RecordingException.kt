package music.jiminy

class LockedForRecordingException : Exception(
    message = "Jiminy is recording. Only allowed operation: Stop recording ($WS_STOP_RECORDING)"
)
