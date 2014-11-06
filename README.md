TK-API-NG
=========

wrapper around the betfair api. You need to create & upload

1.) appid 
2.) certificate 

Detailed here:
  https://api.developer.betfair.com/services/webapps/docs/display/1smk3cen4v3lu3yomq5qye0ni/Non-Interactive+%28bot%29+login

I uploaded a "pem" certificate file to betfair but actually use a pkcs12 certificate file in the code (also shown via that link above).

Once you have an appid & certificate, all you need to do is change these values in "BetfairFace.java":

	private String appid = "xxxxxxx"; //my appid
	private String bfun = "xxxxxxx"; //my betfair username
	private String bfpw = "xxxxxxx"; //my betfair password
	private String ctpw = "xxxxxxx"; //my pkcs12 password

...and corrected the java package declarations listed at the top of both java files! Then it should just execute if you have the included jar file on the classpath.


