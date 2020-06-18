package Actions;

import java.io.IOException;
import java.util.List;
import Pages.Portal.*;


import Pages.SAML.PingIdentityPage;
import Pages.SAML.ProxyAutoAuthExtensionPage;
import Pages.SAML.WelcomeToTrustwaveSwgPage;
import Pages.Web.ExampleDotComPage;
import Pages.Web.GenericWebPage;
import Utils.PropertiesFile.PropertiesFile;
import org.openqa.selenium.WebElement;

import Pages.Portal.AddRulePage;
import Pages.Portal.BlockPage;
import Pages.Portal.ConfigurationPage;
import Pages.Portal.DashboardPage;
import Pages.Portal.LoginPage;
import Pages.Portal.PendingChangesPage;
import Pages.Portal.RulesPage;
import Pages.Portal.UpperMenu;

public class CloudActions extends TestActions {

	public static final String SWGConfigurationURL = "/#/operations?menuKey=swg-customer-home&stackKey=";
	public static final String SWGPoliciesURL = "/#/operations?menuKey=swg-customer-home&stackKey=swg-policies";
	public static final String SWGPolicyTestURL = "/#/operations?menuKey=swg-policy-test&stackKey=";

	public CloudActions() {
		super();
	}

	public void Login(String userName, String password) {
		try {
			LoginPage login = new LoginPage();
			DashboardPage dash = new DashboardPage();

			login.WaitUntilTitleAppearAndPageLoad("Trustwave Fusion");
			login.UserName.SetText(userName);
			login.Password.SetText(password);
			login.LoginButton.click();

			//wait until page load
			dash.WaitUntilObjectClickable(DashboardPage.dashboradByID,120);
		}
		catch (Exception e){

		}

	}


	public void SelectCustomerAtConfigurationPage(String customerNameOrID) throws IOException {
		ConfigurationPage conf = new ConfigurationPage();
		//if it is not production portal search the costumer name
		//if it is production - costumer name search do not exist
		if (!PropertiesFile.isProduction()) {
			conf.WaitUntilObjectClickable(ConfigurationPage.costumerNameBoxByID);
			conf.customerNameBox.SetText(customerNameOrID + "\n");
		}

		conf.WaitUntilObjectClickable(conf.GetFoundCustomerRowBy(customerNameOrID));
		conf.GetFoundCustomerRowElement(customerNameOrID).click();

	}


	//After customer is selected - adds url block rule
	public void DeleteRules(String url) {
		ConfigurationPage conf = new ConfigurationPage();

		conf.WaitUntilPageLoad();
		conf.WaitUntilObjectClickable(ConfigurationPage.categoriesGridByID);
		conf.WaitUntilObjectDisappear(ConfigurationPage.subCategorySpinnerByXpath);


		conf.rules.click();

		RulesPage rules = new RulesPage();
		List<WebElement> list = rules.GetAllRules(url);
		for (int i = 0; i < list.size(); i++) {
			list.get(i).click();
			rules.delete.click();
		}
		if (list.size() > 0)
			CommitPendingChanges();
	}


	//After customer is selected - adds url block rule
	public void AddRule(String url) {
		ConfigurationPage conf = new ConfigurationPage();

		conf.WaitUntilPageLoad();
		conf.WaitUntilObjectClickable(ConfigurationPage.categoriesGridByID);
		conf.WaitUntilObjectDisappear(ConfigurationPage.subCategorySpinnerByXpath);


		conf.rules.click();
		conf.plusButton.click();

		AddRulePage addRule = new AddRulePage();

		addRule.WaitUntilObjectClickable(AddRulePage.RuleTypeByID);

		addRule.ruleType.SetValue("Custom URL");
		addRule.Action.SetValue("Block");
		addRule.URL.SetText(url);
		addRule.Submit.click();

		CommitPendingChanges();
	}

	public void CheckBlockedPage() {
		if (IsPageBlock())
			return;
		org.testng.Assert.fail("Page is not Blocked. Not as expected to be!");
	}


	public void CheckNOTBlockedPage(String site) {

		GenericWebPage page=null;
		switch (site.toLowerCase()){
			case "example.com":
				page = new ExampleDotComPage();
				break;
			default:
				org.testng.Assert.fail("Expected site: \""+ site +"\" Could not find site at source folder Pages.Web classes." );

		}

		try {
			if (page.isPageAppearAsExpected())
				return;
		} catch (Exception e) {
			//e.printStackTrace();
			org.testng.Assert.fail("Page do not appear as expected to be! Expected site: "+ site);

		}


	}

	public boolean IsPageBlock() {
		try {

			BlockPage block = new BlockPage();
			if (block.pageBlocked_element.getText().compareTo("Page Blocked") != 0)
				return false;
			block.trustwaveImage_element.click();
		} catch (Exception e) {
			return false;
		}

		return true;

	}


	public void FillProxyExtensionData(String username, String password) {
		ProxyAutoAuthExtensionPage page = new ProxyAutoAuthExtensionPage();
		page.userName_element.sendKeys(username);
		page.password_element.sendKeys(password);
		page.save_element.click();
	}

	public void LoginToTrustwaveAndPingIdentityFill (String pingUsername, String pingPassword){
		WelcomeToTrustwaveSwgPage welcome = new WelcomeToTrustwaveSwgPage();
		welcome.connectButton_element.click();
		PingIdentityPage ping = new PingIdentityPage();
		ping.username_element.sendKeys(pingUsername);
		ping.password_element.sendKeys(pingPassword);
		ping.SignIn_element.click();

	}


	public void CommitPendingChanges() {
		UpperMenu menu = new UpperMenu();
		menu.pendingChanes.click();
		PendingChangesPage pending = new PendingChangesPage();
		pending.submit.click();

		menu.WaitUntilObjectDisappear(pending.submitBy);

	}

	public void GotoSWGConfigurationPage(String UrlPrefix) {
		this.SetApplicationUrl(UrlPrefix + SWGConfigurationURL);
	}

	public void GotoSWGPoliciesPage(String UrlPrefix) {
		//ConfigurationPage conf = new ConfigurationPage();
		//conf.policies.click();
		this.SetApplicationUrl(UrlPrefix + SWGPoliciesURL);
	}


	//Policy Test -Clicks on policy test configure User
	public void ClickOnPolicyTestCofigureUser() {


		PolicyTestPage pol = new PolicyTestPage();

		pol.WaitUntilPageLoad();

		pol.WaitUntilObjectClickable(PolicyTestPage.Trustwave123);


		pol.TrustWaveButton.click();


	}

//Policy Test -Types in the text box of the Trustwave123 tenant  search

	public void TypeInTrustWave123(String tenant) {

		PolicyTestTenant Type123Text1 = new PolicyTestTenant();

		Type123Text1.WaitUntilObjectClickable(PolicyTestTenant.PolicyTestUrlType);

		Type123Text1.tenant.SetText(tenant);

		Type123Text1.WaitUntilObjectClickable(PolicyTestTenant.PolicyTestUrlType);

//clicks on the selected tenant
		Type123Text1.ChooseTenantId();


	}


	//Policy Test Blocked Url

	public void PolicyTestTypeURL(String Block) {
		PolicyTestBlockUrlPage blockPage = new PolicyTestBlockUrlPage();

		blockPage.WaitUntilObjectClickable(PolicyTestBlockUrlPage.PolicyTestUrlType);


		blockPage.Block.SetText(Block);


	}

	//Policy Test- type IP in text box.


	public void PolicyTestTypeIP(String Block) {

		PolicyTestBlockIpPage IpBlock = new PolicyTestBlockIpPage();
		IpBlock.WaitUntilObjectClickable(PolicyTestBlockIpPage.PolicyTestIpType);

		IpBlock.Block.SetText(Block);


	}


	//Policy Test-Click on Go button
	public void ClickOnGo() {


		PolicyTestPage pol = new PolicyTestPage();


		pol.WaitUntilObjectClickable(PolicyTestPage.PolicyTestClickOnGo);


		pol.ClickOnGoButton();


	}


	//Policy Test check if Website is blocked

	public void PageIsBlocked() {

PolicyTestBlockMessage pol = new PolicyTestBlockMessage();
pol.WaitUntilObjectClickable(PolicyTestBlockMessage.BlockMessage);



//		PolicyTestPage pol = new PolicyTestPage();
//	pol.WaitUntilObjectClickable(PolicyTestPage.PolicyCheckIfBlocked);


	}



//Block message
//	public boolean PolicyTestBlock() {
//		try {
//
//			PolicyTestBlockMessage block = new PolicyTestBlockMessage();
//			if (block.BlockMessage_element.getText().compareTo("connect timed out") != 0)
//				return false;
//
//		} catch (Exception e) {
//			return false;
//		}
//
//		return true;
//	}








		//Policy Test -Login Page
	public void GotoSWGPolicyTestPage(String UrlPrefix) {
		this.SetApplicationUrl(UrlPrefix + SWGPolicyTestURL);


	}

}
