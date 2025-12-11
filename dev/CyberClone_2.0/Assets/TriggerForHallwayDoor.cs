using UnityEngine;
using TMPro;
using System.Collections;


public class TriggerForHallwayDoor : MonoBehaviour
{
    ///
    /// 
    /// 
    /// 
    /// 
    [Header("HALLWAY | SCENE TRANSITIONER")]
    [SerializeField]
    [Tooltip("scene loader for the next scene")]
    private string nextNameForScene = "Level2_1";

    [SerializeField]
    [Tooltip("loading delay for music fx to finish playing")]
    private float delayTransition = 0.75f;

    ///
    /// 
    /// 
    ///
    /// 
    /// 
    [Header("HALLWAY | INTERACTION")]
    [SerializeField]
    [Tooltip("press e to continue")]
    private KeyCode interactKey = KeyCode.E;

    [SerializeField]
    [Tooltip("auto transition or require key press when entering")]
    private bool needsKeyPress = true;

    ///
    /// 
    /// 
    /// 
    /// 
    /// 
    [Header("HALLWAY | SHOW ON SCREEN E BUTTON")]
    [SerializeField]
    [Tooltip("pop up text showing e")]
    private TextMeshProUGUI promptText;  // ← FIXED: Was TextMeshProGUI

    [SerializeField]
    [Tooltip("press e to continue prompt")]
    private string promptmsg = "To Continue Press [E]";

    [SerializeField]
    [Tooltip("gonna make here a panel show up in the ui, door opening maybe")]
    private GameObject prmptPanel;


    ///
    /// 
    /// 
    /// 
    /// 
    [Header("HALLWAY | VISUAL FBACK")]
    [SerializeField]
    [Tooltip("colour of character changes to show a trigger effect maybe")]
    private bool triggerOnDwayne = true;


    [SerializeField]
    [Tooltip("scene view trigger is gonna look like this colour")]
    private Color triggerColour = Color.cyan;  // ← FIXED: Was 'color' (lowercase)

    [Header("HALLWAY | AUDIO ADJUST")]
    [SerializeField]
    [Tooltip("why is there boss music playing lol")]
    private AudioClip hallwaySound;

    [SerializeField]
    [Tooltip("transition sound to play")]
    private AudioClip transitionSound;


    ///
    /// 
    /// 
    /// 
    /// 
    /// 
    /// 
    /// 
    private bool dwayneInArea = false;
    private bool triggerForNextLvl = false;
    private AudioSource audioSource;
    private SpriteRenderer spriteRenderer;

    void Start()
    {
        audioSource = GetComponent<AudioSource>();
        if (audioSource == null && (hallwaySound != null || transitionSound != null))
        {
            audioSource = gameObject.AddComponent<AudioSource>();
        }
        
        spriteRenderer = GetComponent<SpriteRenderer>();  // ← FIXED: Added '=' operator

        dontShowPrompt();

        Collider2D col = GetComponent<Collider2D>();
        if (col == null)
        {
            Debug.LogError($"[HallwayTrigger] {gameObject.name} needs like a collider2d component stupid, add a box collider 2d and check 'isTrigger'");  // ← FIXED: Was 'debug.log'
        }
        else if (!col.isTrigger)
        {
            Debug.LogWarning($"[HallwayTrigger] {gameObject.name} collider should be set to is trigger");
        }
        
        Debug.Log($"[HallwayTrigger] it works lets go to next scene {nextNameForScene}");
    }

    void Update()
    {
        if (needsKeyPress && dwayneInArea && !triggerForNextLvl)  // ← FIXED: Was !triggerOnDwayne
        {
            if (Input.GetKeyDown(interactKey))
            {
                TriggerTransition();
            }
        }
    }

    void OnTriggerEnter2D(Collider2D other)  
    {
        //gotta have the player tag as always

        if (other.CompareTag("Player") && !triggerForNextLvl)  
        {
            dwayneInArea = true;
            //then
            Debug.Log($"[HallwayTrigger] dwayne is here!");

            //enter sound for dwayne 
            if (audioSource != null && hallwaySound != null) 
            {
                audioSource.PlayOneShot(hallwaySound);
            }

            //hope this prompts the key press 

            if (needsKeyPress)
            {
                showPrompt();
            }
            else
            {
                TriggerTransition();
            }

            if (triggerOnDwayne && spriteRenderer != null)
            {
                spriteRenderer.color = Color.green;
            }
        }
    }

    void OnTriggerExit2D(Collider2D other) 
    {
        if (other.CompareTag("Player"))
        {
            dwayneInArea = false;
            Debug.Log($"[HallwayTrigger] oh nooo dwayne left :(");

            dontShowPrompt();

            if (triggerOnDwayne && spriteRenderer != null)
            {
                spriteRenderer.color = triggerColour;
            }
        }
    }


    private void TriggerTransition()
    {
        if (triggerForNextLvl)
            return;

        triggerForNextLvl = true;
        Debug.Log($"[HallwayTrigger] going to {nextNameForScene}");

        ///
        /// 
        /// 
        /// 

        if (audioSource != null && transitionSound != null)
        {
            audioSource.PlayOneShot(transitionSound);
        }

        dontShowPrompt();

        StartCoroutine(TransitionToNextScene());
    }
    
    private IEnumerator TransitionToNextScene()
    {
        yield return new WaitForSeconds(delayTransition);

        if (attemptToDesignGameFLow.Instance != null)
        {
            attemptToDesignGameFLow.Instance.LoadScene(nextNameForScene);
        }
        else
        {
            Debug.LogError("[HallwayTrigger] gameflow not found! check main menu for errors");
        }
    }

    private void showPrompt()
    {
        if (promptText != null)
        {
            promptText.text = promptmsg;
            promptText.gameObject.SetActive(true);
        }
        if (prmptPanel != null)
        {
            prmptPanel.SetActive(true);
        }
    }


    ///
    /// 
    /// 0 _ 0
    /// 
    /// 
    //go away prompt 

    private void dontShowPrompt()
    {
        if (promptText != null)
        {
            promptText.gameObject.SetActive(false);  
        }
        if (prmptPanel != null)
        {
            prmptPanel.SetActive(false);
        }
    }

    //almost to my gizmos 

    ///
    /// 
    /// 
    /// 
    /// 
    public void SetNextScene(string sceneName)
    {
        nextNameForScene = sceneName;
        Debug.Log($"[HallwayTrigger] scene changed name to {sceneName}");

        //basically change it here and call the command later
    }


    void OnDrawGizmos()
    {
        Collider2D col = GetComponent<Collider2D>();  
        if (col != null)
        {
            Gizmos.color = dwayneInArea ? Color.green : triggerColour;  

            if (col is BoxCollider2D box)
            {
                Gizmos.matrix = transform.localToWorldMatrix;  
                Gizmos.DrawWireCube(box.offset, box.size);
            }
            else if (col is CircleCollider2D circle)
            {
                Gizmos.DrawWireSphere(transform.position + (Vector3)circle.offset, circle.radius); 
            }

            //so basically ill explain the gizmos code here
            //it will switch between depending if its a circle or a box collider for the chosen use of collider
        }
        
        Gizmos.color = Color.yellow;  //dumb thing Was 'triggerColour.yellow'
        Vector3 arrowStart = transform.position;
        Vector3 arrowEnd = transform.position + transform.up * 2f;
        Gizmos.DrawLine(arrowStart, arrowEnd);
        
        //need to make sure this is smth only unity handles
        #if UNITY_EDITOR
        UnityEditor.Handles.Label(transform.position + Vector3.up * 2.5f, $"→ {nextNameForScene}");
        #endif
    }  


    void OnDrawGizmosSelected()
    {
        //just added hover detail or when selected
        Gizmos.color = Color.cyan;
        Collider2D col = GetComponent<Collider2D>();
        if (col != null && col is BoxCollider2D box)
        {
            Gizmos.matrix = transform.localToWorldMatrix;
            Gizmos.DrawCube(box.offset, box.size * 0.9f);
        }
    }
}