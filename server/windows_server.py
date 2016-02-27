# Windows server

import socket
import win32com.client # installation required http://sourceforge.net/projects/pywin32/files/pywin32/

UDP_IP = socket.gethostbyname(socket.gethostname())	
UDP_PORT = 3012

sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM) 
sock.bind((UDP_IP, UDP_PORT))

shell = win32com.client.Dispatch("WScript.Shell")
def use_key( key):
	if( key != "whitescreen"):
		key = key.upper()
		shell.SendKeys("{"+key+"}")
	else:
		pass # set whitescreen for application
 
print( "Enter the IP address: " + UDP_IP + " into your mobile phone app")
counter = 0
while True:
	data, addr = sock.recvfrom(1024)
	data_string = data.decode(encoding="utf-8") 
	print("received data: " + data_string)
	if( data_string == "stop_server"):
		break
	use_key(data_string)