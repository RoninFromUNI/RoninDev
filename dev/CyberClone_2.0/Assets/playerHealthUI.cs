using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// 
/// health bar ui for dwayne
/// displays current hp with smooth transitions and color changes
/// 
/// </summary>

public class playerHealthUI : MonoBehaviour
{
    [Header("DWAYNE | HEALTH BAR REFERENCES")]
    [SerializeField]
    [Tooltip("the fill image that shows hp amount")]
    private Image hpBarFill;
    
    [SerializeField]
    [Tooltip("background image for the health bar")]
    private Image hpBarBg;
    
    [SerializeField]
    [Tooltip("optional text to show hp numbers like 80/100")]
    private TextMeshProUGUI hpText;
    ///
    ///
    ///
    [Header("DWAYNE | COLOR SETTINGS")]
    [SerializeField]
    [Tooltip("color when hp is high")]
    private Color highHpColor = Color.green;
    
    [SerializeField]
    [Tooltip("color when hp is medium")]
    private Color medHpColor = Color.yellow;
    
    [SerializeField]
    [Tooltip("color when hp is low")]
    private Color lowHpColor = Color.red;
    
    [SerializeField]
    [Tooltip("hp percentage threshold for low health")]
    private float lowHpThreshold = 0.3f; //30%
    
    [SerializeField]
    [Tooltip("hp percentage threshold for medium health")]
    private float medHpThreshold = 0.6f; //60%
    ///
    ///
    ///
    [Header("DWAYNE | TRANSITION SETTINGS")]
    [SerializeField]
    [Tooltip("smooth transition for health bar drain")]
    private bool useSmoothTrans = true;
    
    [SerializeField]
    [Tooltip("speed of smooth transition")]
    private float transSpeed = 5f;
    ///
    ///
    ///
    [Header("DWAYNE | PLAYER REFERENCE")]
    [SerializeField]
    [Tooltip("reference to player health script, auto finds if not set")]
    private playerhealth_Dwayne playerHealth;
    
    [SerializeField]
    [Tooltip("auto find player health on start")]
    private bool autoFindPlayer = true;
    ///
    ///
    ///
    /// <summary>
    /// 
    /// private tracking variables
    /// 
    /// </summary>
    private float targetFillAmt = 1f;
    private float currentFillAmt = 1f;
    private int currentHp;
    private int maxHp;
    
    void Start()
    {
        //auto finding player health if not assigned
        if (autoFindPlayer && playerHealth == null)
        {
            playerHealth = Object.FindFirstObjectByType<playerhealth_Dwayne>();
            if (playerHealth == null)
            {
                Debug.LogError("[player health ui] cant find player health script!");
            }
            else
            {
                Debug.Log("[player health ui] found player health script");
            }
        }
        
        //getting initial hp values
        if (playerHealth != null)
        {
            maxHp = playerHealth.getMaxHP();
            currentHp = playerHealth.getHP();
            InitializeHealthBar();
        }
    }
    
    void Update()
    {
        if (playerHealth == null) return;
        
        //updating hp values from player
        currentHp = playerHealth.getHP();
        float hpPercent = playerHealth.getHPperecent();
        
        //updating health bar
        UpdateHealthBar(hpPercent);
        
        //updating text if assigned
        if (hpText != null)
        {
            hpText.text = $"{currentHp} / {maxHp}";
        }
    }
    
    /// <summary>
    /// 
    /// initializing health bar to full
    /// 
    /// </summary>
    private void InitializeHealthBar()
    {
        if (hpBarFill == null) return;
        
        targetFillAmt = 1f;
        currentFillAmt = 1f;
        hpBarFill.fillAmount = 1f;
        
        UpdateHealthBarColor(1f);
        
        if (hpText != null)
        {
            hpText.text = $"{maxHp} / {maxHp}";
        }
        
        Debug.Log("[player health ui] initialized health bar");
    }
    
    /// <summary>
    /// 
    /// updating health bar fill amount
    /// using smooth transition if enabled
    /// 
    /// </summary>
    private void UpdateHealthBar(float hpPercent)
    {
        if (hpBarFill == null) return;
        
        targetFillAmt = hpPercent;
        
        if (useSmoothTrans)
        {
            //smooth lerp to target
            currentFillAmt = Mathf.Lerp(currentFillAmt, targetFillAmt, Time.deltaTime * transSpeed);
            hpBarFill.fillAmount = currentFillAmt;
        }
        else
        {
            //instant update
            hpBarFill.fillAmount = targetFillAmt;
        }
        
        //updating color based on hp
        UpdateHealthBarColor(hpPercent);
    }
    
    /// <summary>
    /// 
    /// changing health bar color based on hp percentage
    /// green > 60%, yellow 30-60%, red < 30%
    /// 
    /// </summary>
    private void UpdateHealthBarColor(float hpPercent)
    {
        if (hpBarFill == null) return;
        
        if (hpPercent > medHpThreshold)
        {
            hpBarFill.color = highHpColor;
        }
        else if (hpPercent > lowHpThreshold)
        {
            hpBarFill.color = medHpColor;
        }
        else
        {
            hpBarFill.color = lowHpColor;
        }
    }
}