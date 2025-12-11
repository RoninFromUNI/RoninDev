using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// 
/// ui health bar for boss
/// displays current hp as a filling bar
/// updates when boss takes damage
/// 
/// </summary>

public class bossHealthbarForUI : MonoBehaviour
{
    [Header("HEALTH BAR | UI REFERENCES")]
    [SerializeField]
    [Tooltip("the image component for the fill bar")]
    private Image hpBarFill;
    
    [SerializeField]
    [Tooltip("text component for displaying hp numbers")]
    private Text hpText;
    
    [SerializeField]
    [Tooltip("optional background image")]
    private Image hpBarBg;
    ///
    ///
    ///
    [Header("HEALTH BAR | VISUAL SETTINGS")]
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
    private float lowHpThreshold = 0.3f;
    
    [SerializeField]
    [Tooltip("hp percentage threshold for medium health")]
    private float medHpThreshold = 0.6f;
    ///
    ///
    ///
    [Header("HEALTH BAR | ANIMATION")]
    [SerializeField]
    [Tooltip("smooth hp bar drain animation")]
    private bool useSmoothTrans = true;
    
    [SerializeField]
    [Tooltip("speed of hp bar animation")]
    private float transSpeed = 5f;
    
    /// <summary>
    /// 
    /// 
    /// private tracking variables
    /// 
    /// 
    /// </summary>
    private float targetFillAmt = 1f;
    private float currentFillAmt = 1f;
    private int maxHp = 100;
    
    void Update()
    {
        if (useSmoothTrans)
        {
            UpdateSmoothFill();
        }
    }
    
    /// <summary>
    /// 
    /// initialize health bar with max hp value
    /// called by boss controller on start
    /// 
    /// </summary>
    public void InitializeHealthBar(int maxHealth)
    {
        maxHp = maxHealth;
        targetFillAmt = 1f;
        currentFillAmt = 1f;
        
        if (hpBarFill != null)
        {
            hpBarFill.fillAmount = 1f;
            hpBarFill.color = highHpColor;
        }
        
        UpdateHealthText(maxHp, maxHp);
        
        Debug.Log($"[boss ui] health bar initialized | max hp: {maxHp}");
    }
    
    /// <summary>
    /// 
    /// update health bar display when boss takes damage
    /// changes color based on hp percentage
    /// 
    /// </summary>
    public void UpdateHealthBar(int currentHealth, int maxHealth)
    {
        if (hpBarFill == null) return;
        
        float hpPercent = (float)currentHealth / maxHealth;
        targetFillAmt = hpPercent;
        
        if (!useSmoothTrans)
        {
            hpBarFill.fillAmount = hpPercent;
            currentFillAmt = hpPercent;
        }
        
        //update color based on health percentage
        UpdateHealthBarColor(hpPercent);
        
        //update text display
        UpdateHealthText(currentHealth, maxHealth);
    }
    
    /// <summary>
    /// 
    /// smooth animation for health bar drain
    /// 
    /// </summary>
    private void UpdateSmoothFill()
    {
        if (hpBarFill == null) return;
        
        currentFillAmt = Mathf.Lerp(currentFillAmt, targetFillAmt, transSpeed * Time.deltaTime);
        hpBarFill.fillAmount = currentFillAmt;
    }
    
    /// <summary>
    /// 
    /// update health bar color based on hp percentage
    /// green when high, yellow when medium, red when low
    /// 
    /// </summary>
    private void UpdateHealthBarColor(float hpPercent)
    {
        if (hpBarFill == null) return;
        
        if (hpPercent <= lowHpThreshold)
        {
            hpBarFill.color = lowHpColor;
        }
        else if (hpPercent <= medHpThreshold)
        {
            hpBarFill.color = medHpColor;
        }
        else
        {
            hpBarFill.color = highHpColor;
        }
    }
    
    /// <summary>
    /// 
    /// update text display showing current/max hp
    /// 
    /// </summary>
    private void UpdateHealthText(int currentHealth, int maxHealth)
    {
        if (hpText == null) return;
        
        hpText.text = $"{currentHealth} / {maxHealth}";
    }
    
    /// <summary>
    /// 
    /// show or hide the health bar
    /// useful for boss intro/outro
    /// 
    /// </summary>
    public void SetHealthBarVisibility(bool visible)
    {
        gameObject.SetActive(visible);
    }
}