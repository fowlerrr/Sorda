# Blockchain Transactions

### 1) Add service 
	Method : POST Multipart form data
	https://vodafone-api.nuco.io/vodafone/marketplace/addService

	request

	name - mandatory
	details - mandatory
	description - mandatory
	termsAndConditions - mandatory
	isDigital - mandatory
	isOnchain - mandatory
	regularPrice - mandatory if any of onchain or digital is false
	peakPrice - mandatory if any of onchain or digital is false
	oneMonthPrice - mandatory if onchain and digital is true
	sixMonthPrice - mandatory if onchain and digital is true
	unlimitedPrice - mandatory if onchain and digital is true
	pic - optional

### 3) Add account (you can either add client or partner)

	Method : POST Multi part form data
	https://vodafone-api.nuco.io/vodafone/addAccount

	request

	name : text, mandatory
	email : text, mandatory
	password : text, mandatory
	pic : file, optional (max 1 Mb size, only jpg,jpeg and png allowed)

### 5) Buy a new service

	Method : POST application/json
	https://vodafone-api.nuco.io/vodafone/marketplace/buyService

	request

	{
		"contractAddress" : "3da668e7556cd48549b00696edd19dc59df9bd32",
		"price" : "5"
	}

### 6) Cancel offchain service transaction

	Method : POST application/json
	https://vodafone-api.nuco.io/vodafone/marketplace/cancelService

	request

	{
		"transactionId" : 1
	}

### 7) Ship offchain service

	Method : POST application/json
	https://vodafone-api.nuco.io/vodafone/marketplace/shipService

	request

	{
		"transactionId" : 2
	}

### 8) Approve offchain service transaction

	Method : POST application/json
	https://vodafone-api.nuco.io/vodafone/marketplace/approveService

	request

	{
		"transactionId" : 1
	}

### 9) Login

	Method : POST JSON
	https://vodafone-api.nuco.io/vodafone/login

	request

	email : text, mandatory
	password : text, mandatory
	

## Read only endpoints

### 1) Buy Marketplace

	Method : GET 
	https://vodafone-api.nuco.io/vodafone/marketplace/buyMarketplace

	Socket for updates: Example given in Socket-Example.html
	Create socket : var socket = new SockJS('https://vodafone-api.nuco.io/vodafone/marketplace-interface');
	Subscribe for updates : /marketplace/buyMarketplace+email

### 2) Sell Marketplace

	Method : GET 
	https://vodafone-api.nuco.io/vodafone/marketplace/sellMarketplace

	Socket for updates: Example given in Socket-Example.html
	Create socket : var socket = new SockJS('https://vodafone-api.nuco.io/vodafone/marketplace-interface');
	Subscribe for updates : /marketplace/sellMarketplace+email

### 3) Get Audit Trial

	Method : GET
	https://vodafone-api.nuco.io/vodafone/marketplace/getAuditTrial

	Socket for updates: Example given in Socket-Example.html
	Create socket : var socket = new SockJS('https://vodafone-api.nuco.io/vodafone/marketplace-interface');
	Subscribe for updates : /marketplace/getAuditTrial

### 4) Get Parnter Info

	Method : GET
	https://vodafone-api.nuco.io/vodafone/marketplace/getPartnerInfo

	Socket for updates: Example given in Socket-Example.html
	Create socket : var socket = new SockJS('https://vodafone-api.nuco.io/vodafone/marketplace-interface');
	Subscribe for updates : /marketplace/getPartnerInfo+email

### 5) Get Admin wallet (Admin only)

	Method : GET
	https://vodafone-api.nuco.io/vodafone/marketplace/getAdminInfo

	Socket for updates: Example given in Socket-Example.html
	Create socket : var socket = new SockJS('https://vodafone-api.nuco.io/vodafone/marketplace-interface');
	Subscribe for updates : /marketplace/getAdminInfo

### 6) Get Icon / data files

	Method : GET
	https://vodafone-api.nuco.io/vodafone/getIcon/7df4a4432d9a6af29e9c568c6714a7d1ed8845ced953482e45587eb4217e2d59.jpg

### 7) Get Partner Info (For Admin only)

	Method : GET
	https://vodafone-api.nuco.io/vodafone/marketplace/getPartnerInfo?partnerEmail=shell@gmail.com

### 8) Get Partner list (For Admin only)

	Method : GET
	https://vodafone-api.nuco.io/vodafone/marketplace/partnerList

	Socket for updates: Example given in Socket-Example.html
	Create socket : var socket = new SockJS('https://vodafone-api.nuco.io/vodafone/marketplace-interface');
	Subscribe for updates : /marketplace/partnerList

### 9) All Partner Transactions (Admin only)

	Method : GET
	https://vodafone-api.nuco.io/vodafone/marketplace/getAllPartnerTransactions

	Socket for updates: Example given in Socket-Example.html
	Create socket : var socket = new SockJS('https://vodafone-api.nuco.io/vodafone/marketplace-interface');
	Subscribe for updates : /marketplace/getAllPartnerTransactions

### 10) Get reports

	Method : GET
	https://vodafone-api.nuco.io/vodafone/marketplace/getReports	