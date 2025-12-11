using UnityEngine;
using UnityEngine.UI;

public class mainmenucontroller : MonoBehaviour
{
    //need to again establish my headers, finally understanding it now
    [Header (" UI | references for buttons and toggles")]
    public Button playButton; 
    public Button optionsButton; 
    public Button creditsButton; 
    public Button quitButton;
    public Toggle debugShortcutsToggle; 
    ///
    /// 
    /// 
    /// 
    [Header ("DEBUGGING | options")]
    public bool skipIntroForTest = true; 
    private bool debugShortcutsEnabled = false; //setting it default as false

    ///
    /// 
    /// 
    /// 
    [Header("SCENES | Names for scenes)")]
    public string introSceneName = "IntroScene ";
    public string level1SceneName = "MainLevel1";


    private void Start()
    {
        playButton.onClick.AddListener(OnPlayCLicked); 
        if (optionsButton != null)
        optionsButton.onClick.AddListener(OnOptionsClicked); 

        if(creditsButton!= null)
        {
            creditsButton.onClick.AddListener(OnCreditSClicked);

        }
        if (quitButton!= null)
        quitButton.onClick.AddListener(OnQUitClicked); 


        //greeeaat a debug is needed, if it couldve been simpler. 

        if (debugShortcutsToggle!= null)
        {
            debugShortcutsToggle.onValueChanged.AddListener(OnDebugToggleChanged); 
            debugShortcutsEnabled = debugShortcutsToggle.isOn; 
        }
    } 
    private void OnPlayCLicked()
    {
        if(skipIntroForTest)
        {
            attemptToDesignGameFLow.Instance.LoadScene(level1SceneName); 

        }
        else
        {
            attemptToDesignGameFLow.Instance.LoadScene(introSceneName); 
        }
    }

    private void OnOptionsClicked()
    {
        attemptToDesignGameFLow.Instance.settingsPageMidGame();

    }
    private void OnCreditSClicked()
    {
        attemptToDesignGameFLow.Instance.creditsActivaton(); 
    }

    private void OnQUitClicked()
    {
        attemptToDesignGameFLow.Instance.Quitter(); 
    }

    private void OnDebugToggleChanged(bool isEnabled)
    {
        debugShortcutsEnabled=isEnabled;
        Debug.Log($"debug shortcuts {(isEnabled ? "ENABLED": "DISABLED")}"); 


    }


    private void Update()
    {
        if (!debugShortcutsEnabled) return; 

        //1 hopefully loads level1

        if (Input.GetKeyDown(KeyCode.Alpha1))
        {
            Debug.Log("Loading level 1"); 
           attemptToDesignGameFLow.Instance.LoadScene("Level1"); 
        }
        // 2 should enable level 2 immedientely

        if(Input.GetKeyDown(KeyCode.Alpha2))
        {
            Debug.Log("loading level2"); 
        }

        if (Input.GetKeyDown(KeyCode.Alpha3))
        {
            Debug.Log("loading level 3"); 
            attemptToDesignGameFLow.Instance.LoadScene("Level 3"); //thats if i get her 
        } 
    }
    
}