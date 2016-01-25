# Windows server

import socket
import win32com.client # installation required http://sourceforge.net/projects/pywin32/files/pywin32/

UDP_IP = socket.gethostbyname(socket.gethostname())	
UDP_PORT = 3012

sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM) 
sock.bind((UDP_IP, UDP_PORT))


shell = win32com.client.Dispatch("WScript.Shell")
def use_key( key):
	key = key.upper()
	shell.SendKeys("{"+key+"}")
 
print( "Enter the IP address: " + UDP_IP + " into your mobile phone app")
counter = 0
while counter < 10:
	data, addr = sock.recvfrom(1024)
	data_string = data.decode(encoding="utf-8") 	
	use_key(data_string)
	print("received data: " + data_string)
	counter += 1