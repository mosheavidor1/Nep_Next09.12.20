package Pages.Portal;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;

public class BlockPage extends GenericPage {
	
	private static final String  trustwaveImageID1 = "backGrdImg_Id";
	public static final By trustwaveImageIID = By.id(trustwaveImageID1);

	private static final String  pageBlockedID = "backGrdText_Id";
	public static final By pageBlockedTextByID = By.id(pageBlockedID);

	//Trustwave image
	@FindBy(how=How.ID,using=trustwaveImageID1)
	public WebElement trustwaveImage_element;

	//Trustwave image
	@FindBy(how=How.ID,using=pageBlockedID)
	public WebElement pageBlocked_element;

	
	
	

}
