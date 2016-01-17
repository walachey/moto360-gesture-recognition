#!/usr/bin/python3

import socket
from subprocess import Popen, PIPE
import fcntl
import struct

# Linux only
def get_ip_address(ifname): 
	s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	byte_string = bytes(ifname[:15], 'ascii')
	return socket.inet_ntoa(fcntl.ioctl(
			s.fileno(),0x8915,  # SIOCGIFADDR
			struct.pack( '256s', byte_string)
	)[20:24])

def keypress(sequence):
	sequence = bytes(sequence, "ascii")
	p = Popen(['xte'], stdin=PIPE) # the tool 'xautomation' for 'X' is required
	p.communicate(input=sequence)

UDP_IP =  get_ip_address("wlan0") 
UDP_Port = 3012

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(( UDP_IP, UDP_Port))

print( "Enter the IP address: " + UDP_IP + " into your mobile phone app")
while True:
	data, addr = sock.recvfrom(1024)
	data_string = data.decode(encoding="utf-8") 			
	print("received data: " + data_string)
	if data_string == 'left':
		keypress("key Left ") 
	if data_string == 'right':
		keypress("key Right ")                                
