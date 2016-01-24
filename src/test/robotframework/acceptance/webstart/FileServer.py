from __future__ import print_function
import sys
IS_PYTHON3 = sys.version_info[0] >= 3
if IS_PYTHON3:
    from http.server import SimpleHTTPRequestHandler
    from socketserver import TCPServer, ThreadingMixIn
else:
    from SimpleHTTPServer import SimpleHTTPRequestHandler
    from SocketServer import TCPServer, ThreadingMixIn
from os import chdir, path

import threading
import time

HTTP_PORT = 14563

class CustomHandler(SimpleHTTPRequestHandler):

    def log_message(self, format, *args):
        pass

    # These methods get rid of errors messages caused by javaws closing the socket immediately
    def handle_one_request(self):
        try:
            SimpleHTTPRequestHandler.handle_one_request(self)
        except:
            pass

    def finish(self):
        try:
            SimpleHTTPRequestHandler.finish(self)
        except:
            pass


class FileServer(ThreadingMixIn, TCPServer):
    allow_reuse_address = True

    def __init__(self):
        pass

    def start(self):
        TCPServer.__init__(self, ('localhost', int(HTTP_PORT)), CustomHandler)
        self.RESOURCE_LOCATION = path.abspath(path.dirname(__file__))
        print("Server serving from DocumentRoot:" + self.RESOURCE_LOCATION)
        chdir(self.RESOURCE_LOCATION)
        server_thread = threading.Thread(name='test_file_server', target=self.serve_forever)
        server_thread.daemon = True
        server_thread.start()

    def stop(self):
        if hasattr(self, 'shutdown'):
            self.shutdown()
        else:
            self.server_close()
        print("Server stopped")

if __name__ == '__main__':
    fs = FileServer()
    fs.start()
    while True:
        time.sleep(10)
#    fs.stop()
