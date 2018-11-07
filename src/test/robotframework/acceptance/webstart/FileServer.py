from __future__ import print_function
import os
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

    # Added this method from Jython's SimpleHTTPRequestHandler for compatibility with Jython on Windows OS
    def send_head(self):
        """Common code for GET and HEAD commands.

        This sends the response code and MIME headers.

        Return value is either a file object (which has to be copied
        to the outputfile by the caller unless the command was HEAD,
        and must be closed by the caller under all circumstances), or
        None, in which case the caller has nothing further to do.

        """
        path = self.translate_path(self.path)
        f = None
        if os.path.isdir(path):
            if not self.path.endswith('/'):
                # redirect browser - doing basically what apache does
                self.send_response(301)
                self.send_header("Location", self.path + "/")
                self.end_headers()
                return None
            for index in "index.html", "index.htm":
                index = os.path.join(path, index)
                if os.path.exists(index):
                    path = index
                    break
            else:
                return self.list_directory(path)
        ctype = self.guess_type(path)
        try:
            # Always read in binary mode. Opening files in text mode may cause
            # newline translations, making the actual size of the content
            # transmitted *less* than the content-length!
            f = open(path, 'rb')
        except IOError:
            self.send_error(404, "File not found")
            return None
        self.send_response(200)
        self.send_header("Content-type", ctype)
        try:
            fs = os.fstat(f.fileno())
        except (OSError, AttributeError):
            # Jython on Windows lands here when f.fileno() is invalid
            fs = os.stat(path)
        self.send_header("Content-Length", str(fs[6]))
        self.send_header("Last-Modified", self.date_time_string(fs.st_mtime))
        self.end_headers()
        return f


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
