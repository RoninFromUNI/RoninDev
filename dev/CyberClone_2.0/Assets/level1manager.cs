using System.Collections;

using TMPro;

using UnityEngine;

public class level1manager : MonoBehaviour
{
    ///header timeeeee 
    /// 
    [Header("LEVEL 1 | LEVEL ADUIO")]
    public AudioSource audioSrc; 
    public AudioClip LvlCompleteSfx; 
    ///
    /// 
    /// 
    [Header ("LEVEL 1 | SCENE TRANSITION")]
    public string nextSceneName = "Level1ToLevel2Hallway"; 

    public float transitionDelay = 2.5f;
    ///
    /// 
    /// 
    [Header ("LEVEL 1 | UI REFERENCES")]

    public TextMeshProUGUI killCountTxt; 
    public GameObject levelCompletePanel; 
    /// <summary>
    /// 
    /// 
    /// 
    
    [Header("LEVEL 1 | COMPLEtION REQUIREMENTS ")]
    [SerializeField]
    [Tooltip("number of enemies needed to kill")]

    private int requiredKillCount = 10; 
    private int currentKillCount = 0; 

    private bool lvlComplete = false;
    

    private void Awake()
    {
        //need to reactivate my epidemic subscription for this argghhh 

        audioSrc= GetComponent<AudioSource>(); 

    if (audioSrc == null && LvlCompleteSfx!=null)
        {   
             audioSrc = gameObject.AddComponent<AudioSource>();
         }
    }

    public void OnEnable()
    {
        Isometricenemyai.OnAnyEnemyDeath += HandleEnemyDeath; 
        Debug.Log("[Level1Manager] enemey death is linked to event");
    }

    private void OnDisable()
    {
        Isometricenemyai.OnAnyEnemyDeath-= HandleEnemyDeath;
        Debug.Log("[Level1Manager] this should unsub from enemy death events");
    } 

    void Start()
    {
        UpdateUI(); 
        if (levelCompletePanel != null)
        {
            levelCompletePanel.SetActive(false); 
        }
        Debug.Log($"[level1Manager] level 1 has actually started, lets go! - KILLS REQUIRED {requiredKillCount} enemies to progress onwards");


    }
    ///
    /// 
    /// soo thi sis the enemy death handling which if any enemy dies within scene, the onanydemath triggers
    /// 
    private void HandleEnemyDeath(GameObject enemy)
    {
        if (lvlComplete) return; 

        currentKillCount++; 
        Debug.Log($"[Level1Manager] so the enemy killed is {currentKillCount}/{requiredKillCount}");

        UpdateUI(); 
        
        if (currentKillCount >= requiredKillCount)
        {
            CompleteLevel(); 
        }
    }

    // dont think i need a else statement for that lol

    private void UpdateUI()
    {
        if(killCountTxt != null)
        {
            killCountTxt.text= $"E N E M I E S   E L I M I N A T E D : {currentKillCount} / {requiredKillCount}";
        }
    }

    private void CompleteLevel()
    {
        lvlComplete = true; 
        Debug.Log("[Level1Manager] Level 1 has been complete!");

        //FINALLY I CAN DO SOUNDS!
        if (audioSrc!= null && LvlCompleteSfx != null)
        {
            audioSrc.PlayOneShot(LvlCompleteSfx);
        }

        if(levelCompletePanel!= null)
        {
            levelCompletePanel.SetActive(true); 
        }

        StartCoroutine(MoveToNextScene()); 
    }

    private IEnumerator MoveToNextScene()
    {
        yield return new WaitForSeconds(transitionDelay);

        if(attemptToDesignGameFLow.Instance!=null)
        {
            attemptToDesignGameFLow.Instance.LoadScene(nextSceneName);
        }

        else
        {
            Debug.LogError("[Level1Manager] the gameflow worker doesnt work or it is missing!");
        }


    }


    ///
    /// 
    /// 
    /// ANXIETY BREAKKKKK!!
    /// 
    /// 
    /// 
    /// 
    
    [ContextMenu("Forcing my level to complete")]
    public void ForceCompleteLevel()
    {
        Debug.Log("[Level1Manager] force completed");
        currentKillCount=requiredKillCount; 
        CompleteLevel(); 
    }

    ///this stuff is optional really if derek really likes this game 
    /// additional things to test the game furhter
    [ContextMenu("Add one kill")]
    public void AddingKillsCheat()
    {
        if(lvlComplete) return; 
        currentKillCount++; 
        UpdateUI(); 
        Debug.Log($"[Level1Manager] cheat has been activated...cheater...added 1 kill. Progress: {currentKillCount}/{requiredKillCount}");

        if(currentKillCount>=requiredKillCount)
        {
            //in case it keeps adding kills and messes up my kilcount organisations
            //which i can predict will happen anyways lol

            CompleteLevel();
        }
    }

    public int GetCurrentKillsCount() => currentKillCount;
    public int GetRequiredKillsCount() => requiredKillCount; 


    public float GetProgressPercentage() =>(float)currentKillCount/requiredKillCount; 
    public bool IsLevelComplete() => lvlComplete; 
}

