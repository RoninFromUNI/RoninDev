using System.Collections;
using TMPro;
using UnityEngine;


public class LevelFinalManager : MonoBehaviour
{
    /// <summary>
    /// 
    /// 
    /// 
    /// 
    /// 
    /// </summary>
    [Header("FINAL LEVEL | LEVEL AUDIO")]
    [SerializeField]
    [Tooltip("audio source for level complete sound")]
    private AudioSource audioSrc;

    [SerializeField]
    [Tooltip("victory sound when boss is defeated")]
    private AudioClip lvlCompleteSfx;

    [SerializeField]
    [Tooltip("optional boss fight music that plays during battle")]
    private AudioClip bossBattleMusic;
    ///
    /// 
    /// 
    /// 
    /// 
    /// 
    [Header("FINAL LEVEL | SCENE TRANSITION")]
    [SerializeField]
    [Tooltip("scene to load after boss defeat, should be credits")]
    private string creditsSceneName = "Credits";

    [SerializeField]
    [Tooltip("delay before transitioning to credits")]
    private float transitionDelay = 3f;
    ///
    /// 
    /// 
    /// 
    /// 
    /// 
    /// 
    [Header("FINAL LEVEL | UI REFERENCES")]
    [SerializeField]
    [Tooltip("text showing boss health or status")]
    private TextMeshProUGUI bossStatusTxt;

    [SerializeField]
    [Tooltip("panel that shows when boss is defeated")]
    private GameObject victoryPanel;
    ///
    /// 
    /// 
    /// 
    /// 
    [Header("FINAL LEVEL | BOSS REFERENCE")]
    [SerializeField]
    [Tooltip("reference to the boss controller in scene")]
    private isoBossController bossController;

    [SerializeField]
    [Tooltip("auto find boss if not assigned")]
    private bool autoFindBoss = true;
    ///
    /// 
    /// 
    /// 
    private bool lvlComplete = false;
    private bool bossDefeated = false;


    private void Awake()
    {
        audioSrc = GetComponent<AudioSource>();

        if (audioSrc == null && lvlCompleteSfx != null)
        {
            audioSrc = gameObject.AddComponent<AudioSource>();

            //this should all setup my audio
        }

        if (autoFindBoss && bossController == null)
        {
            bossController = Object.FindFirstObjectByType<isoBossController>();
            if (bossController != null)
            {
                Debug.Log("[LevelFinalManager] found boss controller automatically");
            }
            else
            {
                Debug.LogError("[LevelFinalManager] no boss controller found in scene!");
            }
        }
    }

    void Start()
    {

        if (victoryPanel != null)
        {
            victoryPanel.SetActive(false);
        } //this barely works but supposed to hide the panel in the start


        if (audioSrc != null && bossBattleMusic != null)
        {
            audioSrc.clip = bossBattleMusic;
            audioSrc.loop = true;
            audioSrc.Play();
            Debug.Log("[LevelFinalManager] boss battle music started"); //ive already assigned some parts/
            //of audio for the boss, but not all of it is filled in my serializefields
        }

        UpdateUI();

        Debug.Log("[LevelFinalManager] the untitled9 knows youre here, quick defeat him!");
    }

    void Update()
    {
        //continuously check if boss is defeated
        if (!bossDefeated && bossController != null)
        {
            if (bossController.IsDefeated())
            {
                HandleBossDefeat();
            }
            else
            {
                //update UI with boss status while alive
                UpdateUI();
            }
        }
    }
    private void HandleBossDefeat()
    {
        if (lvlComplete) return; //prevent multiple triggers

        bossDefeated = true;

        Debug.Log("[LevelFinalManager] wow you actually did it!");

        CompleteLevel();
    }
    private void UpdateUI()
    {
        if (bossStatusTxt == null || bossController == null) return;

        if (bossController.IsDefeated())
        {
            bossStatusTxt.text = "V I C T O R Y !";
        }
        else if (bossController.IsVulnerable())
        {
            int currentHp = bossController.GetCurrentHealth();
            int maxHp = bossController.GetMaxHealth();
            float timeLeft = bossController.GetVulnerableTimeRemaining();

            bossStatusTxt.text = $"B O S S   V U L N E R A B L E ! \nHP: {currentHp}/{maxHp} | Time: {timeLeft:F1}s"; //this is literally the time player has to kill the oss or else
        }
        else
        {

            bossStatusTxt.text = "D E F E A T   E N E M I E S   T O   O P E N   B O S S"; //always will appear when coming back to invulnerable
        }
    }


    private void CompleteLevel() //vic sequence and sound being played here
    {
        lvlComplete = true;
        Debug.Log("[LevelFinalManager] final level complete! transitioning to credits...");


        if (audioSrc != null && audioSrc.isPlaying)
        {
            audioSrc.Stop();
        } //had a problem where the music kept playing, i solved it here

        //cue the imaginary trumpets!
        if (audioSrc != null && lvlCompleteSfx != null)
        {
            audioSrc.PlayOneShot(lvlCompleteSfx);
        }


        if (victoryPanel != null)
        {
            victoryPanel.SetActive(true); //doesnt work.
        }


        UpdateUI();
        StartCoroutine(MoveToCredits()); //roll to credits!
    }
    private IEnumerator MoveToCredits() //didnt want to utilise the update for this and rather use coroutines, so much cleaner
    {
        yield return new WaitForSeconds(transitionDelay);

        if (attemptToDesignGameFLow.Instance != null)
        {
            attemptToDesignGameFLow.Instance.LoadScene(creditsSceneName);
        }
        else
        {
            Debug.LogError("[LevelFinalManager] why did you delete the gameflow manager!!");
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


    [ContextMenu("Force complete final level")] //incase i get impatient. 
    public void ForceCompleteLevel()
    {
        Debug.Log("[LevelFinalManager] force completed - skipping to credits");
        bossDefeated = true;
        CompleteLevel();
    }


    [ContextMenu("Instantly kill boss")] //still debugging the boss further
    public void InstantKillBoss()
    {
        if (bossController == null)
        {
            Debug.LogWarning("[LevelFinalManager] no boss reference to kill");
            return;
        }

        //deal massive damage to kill boss instantly
        int bossHp = bossController.GetCurrentHealth();
        bossController.TakeDamage(bossHp + 100); //overkill to make sure it dies

        Debug.Log("[LevelFinalManager] boss has been instantly killed via debug menu");
    }

    public bool IsBossDefeated() => bossDefeated;
    public bool IsLevelComplete() => lvlComplete;
    public isoBossController GetBossController() => bossController;
}

///
/// 
/// 
/// so if youre currently not aware by now, but my structure of code will always be private var on top and public getters on bottom
/// and then in the middle is sound and attakc management. 