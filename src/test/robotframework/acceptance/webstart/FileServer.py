from SimpleHTTPServer import SimpleHTTPRequestHandler
from os import chdir, path
from SocketServer import TCPServer, ThreadingMixIn
import threading
import time

HTTP_PORT = 14563

class CustomHandler(SimpleHTTPRequestHandler):

    def log_message(self, format, *args):
        pass

class FileServer(ThreadingMixIn, TCPServer):
    allow_reuse_address = True

    def __init__(self):
        pass

    def start(self):
        TCPServer.__init__(self, ('localhost', int(HTTP_PORT)), CustomHandler)
        self.RESOURCE_LOCATION = path.abspath(path.dirname(__file__))
        print "Server serving from DocumentRoot:" + self.RESOURCE_LOCATION
        chdir(self.RESOURCE_LOCATION)
        server_thread = threading.Thread(target=self.serve_forever)
        server_thread.daemon = True
        server_thread.start()

    def stop(self):
        self.server_close()
        print "Server stopped"

if __name__ == '__main__':
    fs = FileServer()
    fs.start()
    while True:
        time.sleep(10)
#    fs.stop()
