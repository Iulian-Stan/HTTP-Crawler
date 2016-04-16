# HTTPCrawler

It implements two base operations:
 1. **Robots Exclusion Protocol** (REP) handling , and 
 2. **HTML** page content download and storage through **HTTP**


## REP

REP is devided two core elements:
* robots.txt located in the root directory of the site
* meta tags presented in the *head* section of the web page

For more details about these you can find on [robotstxt.org](http://www.robotstxt.org/)


## HTTP

HTTP functions as a request–response protocol in the client–server computing model.
As in the prevoius [poject](https://github.com/Iulian-Stan/DNSResolver) the request 
is created "manualy". Although both **DNS** and **HTTP** are application level ptotocols 
in the **IP** stack, the last one is text based, so constructing human readable 
**HTTP** header is much easier. There are a few spects that require attention:
* Before making the request it is necessary to make sure that **GET** option is allowed,
as it let us download the content.
* Due to multiple **HTTP** response codes, each one should be handled accordingly.

All these are explained in details in [RFC 2616](https://www.ietf.org/rfc/rfc2616.txt)


It is the 2nd project of a series leading to the implementation of a **Web Robot**
 1. [DNSResolver](https://github.com/Iulian-Stan/DNSResolver) 
 2. [HTTPCrawler](https://github.com/Iulian-Stan/HTTPCrawler) 
 3. [HTMLParser](https://github.com/Iulian-Stan/HTMLParser)
 4. [WebRobot](https://github.com/Iulian-Stan/WebRobot) - work in progress
